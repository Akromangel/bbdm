package kuchma.mike.bbdm.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
    packages = "kuchma.mike.bbdm",
    importOptions = ImportOption.DoNotIncludeTests.class
)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
class OnionArchitectureTest {
    private static final String APPLICATION_LAYER = "Application";
    private static final String DOMAIN_LAYER = "Domain";

    private static final ArchRule ONION_ARCHITECTURE = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Presentation").definedBy("..presentation..")
        .layer(APPLICATION_LAYER).definedBy("..application..")
        .layer(DOMAIN_LAYER).definedBy("..domain..")
        .layer("Infrastructure").definedBy("..infrastructure..")
        .whereLayer("Presentation").mayOnlyAccessLayers(APPLICATION_LAYER)
        .whereLayer(APPLICATION_LAYER).mayOnlyAccessLayers(DOMAIN_LAYER)
        .whereLayer("Infrastructure").mayOnlyAccessLayers(APPLICATION_LAYER, DOMAIN_LAYER)
        .whereLayer(DOMAIN_LAYER).mayNotAccessAnyLayer();

    @ArchTest
    static final ArchRule productionCodeFollowsOnionArchitecture = ONION_ARCHITECTURE;
}
