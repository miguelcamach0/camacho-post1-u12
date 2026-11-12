# Sistema de Solicitudes de Reembolso de Gastos Corporativos

## Descripción del proyecto

Este proyecto desarrolla un sistema de gestión de solicitudes de reembolso de gastos corporativos utilizando Java 17 y Spring Boot 3.x.

El sistema permitirá registrar solicitudes de empleados, validar las condiciones necesarias para su aprobación, calcular el monto de reembolso según la categoría del gasto y notificar los cambios de estado a las partes interesadas.

El propósito de la actividad es analizar los requisitos funcionales y aplicar patrones de diseño únicamente donde exista una necesidad real de variación o extensión del comportamiento, evitando introducir complejidad innecesaria.

## Diseño Propuesto

Antes de implementar las funcionalidades, se identificaron tres puntos de variabilidad que requieren soluciones independientes.

### RF1 — Validación en varias etapas

**Punto de variabilidad:** El número y tipo de verificaciones que debe superar cada solicitud puede aumentar con nuevos requisitos del área de Finanzas, como el límite anual acumulado por empleado.

**Patrón seleccionado:** Chain of Responsibility (Cadena de Responsabilidad).

**Justificación:** Se selecciona Chain of Responsibility porque permite ejecutar validaciones independientes de forma secuencial, detener el proceso ante un incumplimiento y agregar nuevas verificaciones sin modificar las existentes ni concentrarlas en un único bloque condicional.

### RF2 — Cálculo distinto por categoría de gasto

**Punto de variabilidad:** La fórmula utilizada para determinar el monto aprobado cambia según la categoría del gasto y puede ampliarse cuando se incorporen nuevas categorías.

**Patrón seleccionado:** Strategy (Estrategia).

**Justificación:** Se selecciona Strategy porque encapsula cada fórmula de cálculo en una implementación independiente que puede seleccionarse según la categoría, evitando modificar las fórmulas existentes cuando se agreguen nuevos comportamientos.

### RF3 — Notificación simultánea a varios interesados

**Punto de variabilidad:** Los destinatarios de las notificaciones, sus acciones ante los cambios de estado y la cantidad de interesados pueden variar con el crecimiento del sistema.

**Patrón seleccionado:** Observer (Observador).

**Justificación:** Se selecciona Observer porque permite notificar un mismo cambio de estado a varios interesados con comportamientos distintos sin acoplar el servicio principal a las implementaciones concretas de cada notificador.

## Justificación general del diseño

Los tres patrones seleccionados resuelven necesidades diferentes del sistema: Chain of Responsibility organiza las verificaciones, Strategy permite variar los algoritmos de cálculo y Observer distribuye las notificaciones ante los cambios de estado.

La propuesta mantiene responsabilidades separadas y aplica los principios KISS y YAGNI, ya que cada patrón responde a una variabilidad identificada en los requisitos y no a una necesidad hipotética sin fundamento.

### Decisiones de diseño

## Decisión de diseño 1 — Orden de ejecución de los validadores

**Alternativas evaluadas:** Ejecutar primero la búsqueda de solicitudes duplicadas o dejarla como última verificación de la cadena.

**Decisión tomada:** Se estableció el siguiente orden de validación: documentos soporte, monto máximo permitido y solicitudes duplicadas.

**Justificación:** Se decidió ubicar primero las verificaciones de documentos y monto porque son comprobaciones sencillas que pueden resolverse rápidamente, mientras que la detección de duplicados requiere recorrer la lista de solicitudes pendientes del empleado. De esta manera, cuando una solicitud no tiene documentos o supera el monto permitido, el sistema puede rechazarla sin realizar comparaciones adicionales.

**Ventajas:** Este orden reduce las operaciones innecesarias y permite detectar anticipadamente incumplimientos básicos, mejorando la eficiencia del procesamiento.

**Desventajas:** Al detenerse en el primer error, el sistema no informa todos los incumplimientos de una solicitud en una sola respuesta. Además, si una solicitud presenta varios problemas, será necesario corregir el primero para descubrir los siguientes.

**Alternativa descartada:** Se descartó ejecutar primero la validación de duplicados, porque obligaría a recorrer las solicitudes existentes incluso cuando faltaran documentos o se excediera el monto permitido.

**Conclusión:** El orden seleccionado cumple el principio KISS, mantiene la lógica sencilla y prioriza las validaciones menos costosas. La decisión es adecuada para las necesidades actuales del sistema y podría revisarse si en el futuro se requiere informar todos los errores simultáneamente.

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Data JPA
- H2 Database
- JUnit 5
- ArchUnit 1.x

## Estado del proyecto

Diseño inicial y modelo de dominio de la Parte 1. Las implementaciones de los patrones, el servicio de aplicación, el controlador REST y las pruebas se incorporarán en los siguientes pasos.