package com.skycatdev.nohatfordaground;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class MixinTest {
    @BeforeAll
    static void setupRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testMixinApplication() {
        MixinEnvironment.getCurrentEnvironment().audit();
    }
}
