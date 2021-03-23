/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.tck;

import com.google.common.collect.Iterables;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractMultiVariantNetworkTest {

    private static final String NLOAD2 = "NLOAD";
    private static final String SECOND_VARIANT = "SecondVariant";

    @Test
    public void singleThreadTest() {
        Network network = EurostagTutorialExample1Factory.create();
        final VariantManager manager = network.getVariantManager();
        manager.cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, SECOND_VARIANT);
        manager.setWorkingVariant(SECOND_VARIANT);
        final Generator generator = network.getGenerator("GEN");
        generator.setRegulationMode(RegulationMode.OFF);
        assertSame(RegulationMode.OFF, generator.getRegulationMode());
        manager.setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        assertSame(RegulationMode.VOLTAGE, generator.getRegulationMode());
    }

    @Test
    public void multiThreadTest() throws InterruptedException {
        final Network network = EurostagTutorialExample1Factory.create();
        final VariantManager manager = network.getVariantManager();
        manager.allowVariantMultiThreadAccess(true);

        manager.cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, SECOND_VARIANT);

        final Generator generator = network.getGenerator("GEN");

        manager.setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        generator.setRegulationMode(RegulationMode.VOLTAGE);

        manager.setWorkingVariant(SECOND_VARIANT);
        generator.setRegulationMode(RegulationMode.OFF);

        final RegulationMode[] voltageRegulatorOnInitialVariant = new RegulationMode[1];
        final RegulationMode[] voltageRegulatorOnSecondVariant = new RegulationMode[1];
        final CountDownLatch latch = new CountDownLatch(2); // to sync threads after having set the working variant
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.invokeAll(Arrays.asList(
            () -> {
                manager.setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
                latch.countDown();
                latch.await();
                voltageRegulatorOnInitialVariant[0] = generator.getRegulationMode();
                return null;
            },
            () -> {
                manager.setWorkingVariant(SECOND_VARIANT);
                latch.countDown();
                latch.await();
                voltageRegulatorOnSecondVariant[0] = generator.getRegulationMode();
                return null;
            })
        );
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
        assertSame(RegulationMode.VOLTAGE, voltageRegulatorOnInitialVariant[0]);
        assertSame(RegulationMode.OFF, voltageRegulatorOnSecondVariant[0]);
    }

    @Test
    public void multiVariantTopologyTest() {
        Network network = EurostagTutorialExample1Factory.create();
        VariantManager manager = network.getVariantManager();
        manager.cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, "NEW_VARIANT");
        VoltageLevel vlload = network.getVoltageLevel("VLLOAD");
        Bus nload = vlload.getBusBreakerView().getBus(NLOAD2);
        Load newLoad = vlload.newLoad()
                .setId("NEW_LOAD")
                .setP0(10)
                .setQ0(10)
                .setBus(NLOAD2)
                .setConnectableBus(NLOAD2)
            .add();
        manager.setWorkingVariant("NEW_VARIANT");
        assertNotNull(newLoad.getTerminal().getBusBreakerView().getBus());
        assertEquals(2, Iterables.size(nload.getLoads()));
        newLoad.getTerminal().disconnect();
        assertNull(newLoad.getTerminal().getBusBreakerView().getBus());
        assertEquals(2, Iterables.size(vlload.getLoads()));
        assertEquals(1, Iterables.size(nload.getLoads()));
        manager.setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        assertNotNull(newLoad.getTerminal().getBusBreakerView().getBus());
        assertEquals(2, Iterables.size(vlload.getLoads()));
        assertEquals(2, Iterables.size(nload.getLoads()));
    }

    @Test
    public void variantNotSetTest() throws InterruptedException {
        Network network = EurostagTutorialExample1Factory.create();
        VariantManager manager = network.getVariantManager();
        manager.allowVariantMultiThreadAccess(true);
        assertEquals(VariantManagerConstants.INITIAL_VARIANT_ID, manager.getWorkingVariantId());
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try {
                network.getGenerator("GEN").getTargetP();
                fail();
            } catch (Exception ignored) {
                // ignore
            }
        });
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void variantSetTest() throws InterruptedException {
        Network network = EurostagTutorialExample1Factory.create();
        VariantManager manager = network.getVariantManager();
        manager.allowVariantMultiThreadAccess(true);
        assertEquals(VariantManagerConstants.INITIAL_VARIANT_ID, manager.getWorkingVariantId());
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try {
                manager.setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
                network.getGenerator("GEN").getTargetP();
            } catch (Exception e) {
                fail();
            }
        });
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

}
