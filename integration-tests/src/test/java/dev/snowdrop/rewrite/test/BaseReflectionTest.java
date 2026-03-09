package dev.snowdrop.rewrite.test;

import dev.snowdrop.rewrite.RewriteServiceProxy;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

public class BaseReflectionTest {

    public RewriteServiceProxy proxy;

    @BeforeEach
    public void beforeEach() {
        proxy = new RewriteServiceProxy();
        proxy.setExportDatatables(true);
        proxy.setExclusions(Set.of());
        proxy.setPlainTextMasks(Set.of());
        proxy.setAdditionalJarPaths(List.of());
        proxy.setDryRun(true);
    }
}
