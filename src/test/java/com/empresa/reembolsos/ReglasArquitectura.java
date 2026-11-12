
package com.empresa.reembolsos;

import com.empresa.reembolsos.procesamiento.calculo.CalculadoraReembolso;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifica las restricciones arquitectonicas
 * documentadas en ADR-001.
 */
@AnalyzeClasses(
        packages = "com.empresa.reembolsos",
        importOptions = DoNotIncludeTests.class
)
public class ReglasArquitectura {

    // REGLA 1:
    // Procesamiento no depende de infraestructura,
    // adaptadores REST ni tecnologias de persistencia.

    @ArchTest
    static final ArchRule procesamientoSinPersistenciaNiWeb =
            noClasses()
                .that()
                .resideInAPackage("..procesamiento..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "..infraestructura..",
                    "..adaptadores..",
                    "org.springframework.data..",
                    "jakarta.persistence.."
                );

    // REGLA 2:
    // El controlador accede solamente a sus
    // dependencias permitidas, sin saltar capas.

    @ArchTest
    static final ArchRule controladorSoloUsaServicio =
            classes()
                .that()
                .resideInAPackage("..adaptadores.rest..")
                .and()
                .haveSimpleName(
                    "SolicitudReembolsoController")
                .should()
                .onlyAccessClassesThat()
                .resideInAnyPackage(
                    "..adaptadores.rest..",
                    "..aplicacion..",
                    "..dominio..",
                    "org.springframework.web..",
                    "org.springframework.http..",
                    "java.."
                );

    // REGLA 3:
    // Las calculadoras concretas deben cumplir
    // el contrato del patron Strategy.

    @ArchTest
    static final ArchRule calculadorasImplementanInterfaz =
            classes()
                .that()
                .resideInAPackage(
                    "..procesamiento.calculo..")
                .and()
                .haveSimpleNameStartingWith(
                    "Calculadora")
                .and()
                .areNotInterfaces()
                .should()
                .implement(CalculadoraReembolso.class);

    // REGLA 4:
    // Los notificadores concretos deben cumplir
    // el contrato del patron Observer.

    @ArchTest
    static final ArchRule notificadoresImplementanInterfaz =
            classes()
                .that()
                .resideInAPackage(
                    "..procesamiento.notificacion..")
                .and()
                .haveSimpleNameStartingWith(
                    "Notificador")
                .and()
                .areNotInterfaces()
                .should()
                .implement(ObservadorCambioEstado.class);
}

