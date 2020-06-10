/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dynamicsimulation.tool;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.io.table.*;
import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulation;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.groovy.CurveGroovyExtension;
import com.powsybl.dynamicsimulation.groovy.GroovyCurvesSupplier;
import com.powsybl.dynamicsimulation.groovy.GroovyExtension;
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultSerializer;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.tools.ConversionToolUtils;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
@AutoService(Tool.class)
public class DynamicSimulationTool implements Tool {

    private static final String CASE_FILE = "case-file";
    private static final String CURVES_FILE = "curves-file";
    private static final String PARAMETERS_FILE = "parameters-file";
    private static final String SKIP_POSTPROC = "skip-postproc";
    private static final String OUTPUT_FILE = "output-file";

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getName() {
                return "dynamic-simulation";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run dynamic simulation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE)
                    .desc("the case path")
                    .hasArg()
                    .argName("FILE")
                    .required()
                    .build());
                options.addOption(Option.builder().longOpt(CURVES_FILE)
                    .desc("curves description as Groovy file")
                    .hasArg()
                    .argName("FILE")
                    .build());
                options.addOption(Option.builder().longOpt(PARAMETERS_FILE)
                    .desc("dynamic simulation parameters as JSON file")
                    .hasArg()
                    .argName("FILE")
                    .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE)
                    .desc("dynamic simulation results output path")
                    .hasArg()
                    .argName("FILE")
                    .build());
                options.addOption(Option.builder().longOpt(SKIP_POSTPROC)
                    .desc("skip network importer post processors (when configured)")
                    .build());
                options.addOption(ConversionToolUtils.createImportParametersFileOption());
                options.addOption(ConversionToolUtils.createImportParameterOption());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }

        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE));
        boolean skipPostProc = line.hasOption(SKIP_POSTPROC);
        Path outputFile = null;

        // process a single network: output-file/output-format options available
        if (line.hasOption(OUTPUT_FILE)) {
            outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE));
        }

        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Properties inputParams = ConversionToolUtils.readProperties(line, ConversionToolUtils.OptionType.IMPORT, context);
        ImportConfig importConfig = (!skipPostProc) ? ImportConfig.load() : new ImportConfig();
        Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(),
            importConfig, inputParams);
        if (network == null) {
            throw new PowsyblException("Case '" + caseFile + "' not found");
        }

        DynamicSimulation.Runner runner = DynamicSimulation.find();

        CurvesSupplier curvesSupplier = CurvesSupplier.empty();
        if (line.hasOption(CURVES_FILE)) {
            Path curvesFile = context.getFileSystem().getPath(line.getOptionValue(CURVES_FILE));
            curvesSupplier = createCurvesSupplier(curvesFile, runner.getName());
        }

        DynamicSimulationParameters params = DynamicSimulationParameters.load();
        if (line.hasOption(PARAMETERS_FILE)) {
            Path parametersFile = context.getFileSystem().getPath(line.getOptionValue(PARAMETERS_FILE));
            JsonDynamicSimulationParameters.update(params, parametersFile);
        }

        DynamicSimulationResult result = runner.run(network, curvesSupplier, VariantManagerConstants.INITIAL_VARIANT_ID, context.getShortTimeExecutionComputationManager(), params);

        if (outputFile != null) {
            exportResult(result, context, outputFile);
        } else {
            printResult(result, context);
        }
    }

    private CurvesSupplier createCurvesSupplier(Path path, String providerName) {
        String extension = FilenameUtils.getExtension(path.toString());
        if (extension.equals("groovy")) {
            return new GroovyCurvesSupplier(path, GroovyExtension.find(CurveGroovyExtension.class, providerName));
        } else {
            throw new PowsyblException("Unsupported curves format: " + extension);
        }
    }

    private void printResult(DynamicSimulationResult result, ToolRunningContext context) {
        Writer writer = new OutputStreamWriter(context.getOutputStream());

        AsciiTableFormatterFactory asciiTableFormatterFactory = new AsciiTableFormatterFactory();
        printDynamicSimulationResult(result, writer, asciiTableFormatterFactory, TableFormatterConfig.load());
    }

    private void printDynamicSimulationResult(DynamicSimulationResult result, Writer writer,
        TableFormatterFactory formatterFactory,
        TableFormatterConfig formatterConfig) {
        try (TableFormatter formatter = formatterFactory.create(writer,
            "dynamic simulation results",
            formatterConfig,
            new Column("Result"))) {
            formatter.writeCell(result.isOk());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void exportResult(DynamicSimulationResult result, ToolRunningContext context, Path outputFile) {
        context.getOutputStream().println("Writing results to '" + outputFile + "'");
        DynamicSimulationResultSerializer.write(result, outputFile);
    }
}