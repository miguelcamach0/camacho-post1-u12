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

### Decisión de diseño 2 — Selección de la estrategia de cálculo por categoría
**Problema identificado:** El sistema debe seleccionar la fórmula de reembolso correspondiente a cada categoría de gasto, considerando que el área de Finanzas planea incorporar nuevas categorías en el futuro sin modificar las implementaciones existentes.

**Alternativas evaluadas:**
1. **Selección mediante switch:** Consiste en utilizar una estructura condicional que evalúe la categoría recibida y determine manualmente qué calculadora ejecutar. Aunque es una alternativa sencilla para un conjunto fijo de categorías, obliga a modificar el método de selección cada vez que se incorpora una nueva estrategia.
2. **Selección mediante Map e inyección de dependencias:** Consiste en recibir automáticamente las implementaciones de CalculadoraReembolso registradas por Spring y organizarlas en un mapa cuya clave corresponde a la categoría de gasto.

**Decisión tomada:** Se seleccionó un Map<CategoriaGasto, CalculadoraReembolso> construido a partir de List<CalculadoraReembolso>, aprovechando la inyección de dependencias de Spring y las anotaciones @Component de cada implementación.
**Justificación:** Se eligió este mecanismo porque permite incorporar nuevas fórmulas mediante implementaciones independientes de CalculadoraReembolso sin modificar el código del selector ni las calculadoras existentes, cumpliendo la necesidad de extensibilidad definida en RF2.

**Ventajas:**
- Favorece el principio abierto/cerrado, dado que el comportamiento puede extenderse mediante nuevas implementaciones.
- Evita concentrar todas las fórmulas en una estructura condicional.
- Mantiene separadas las responsabilidades de selección y cálculo.
- Aprovecha el registro automático de componentes de Spring.
- Facilita las pruebas unitarias de cada estrategia de manera independiente.

**Desventajas:**
- El funcionamiento depende del registro correcto de las implementaciones en Spring.
- Si se registran dos estrategias para la misma categoría, la construcción del mapa generará un conflicto que deberá corregirse.
- La selección mediante inyección de dependencias puede ser menos evidente para un desarrollador que no esté familiarizado con Spring.

**Alternativa descartada:** Se descartó el uso de un switch manual porque, aunque sería suficiente para las tres categorías actuales, requeriría modificar el selector cuando se agregaran nuevas fórmulas de cálculo, aumentando el mantenimiento y la posibilidad de errores.
**Consideración de extensibilidad:** La incorporación de una categoría completamente nueva todavía exige declararla en CategoriaGasto y crear su estrategia correspondiente. Sin embargo, no requiere modificar SelectorCalculadora ni las estrategias existentes.
**Conclusión:** El mapa con inyección automática representa la alternativa más adecuada para RF2, porque la variabilidad de las fórmulas es real y está documentada en los requisitos, lo que justifica la aplicación del patrón Strategy sin introducir abstracciones innecesarias.

### Decisión de diseño 4 — Delimitación de dependencias permitidas en el paquete procesamiento

**Problema identificado:** Durante la implementación de las reglas ArchUnit fue necesario determinar qué dependencias externas pueden utilizar las clases del paquete `procesamiento`, sin comprometer la separación de responsabilidades establecida en ADR-001.

**Alternativas evaluadas:**

1. **Prohibir todas las dependencias de Spring:** Impedir cualquier importación del paquete `org.springframework` dentro de `procesamiento`, incluyendo anotaciones como `@Component`.

2. **Prohibir únicamente las dependencias de infraestructura y persistencia:** Permitir las anotaciones de estereotipo utilizadas para registrar los componentes de Spring, pero impedir dependencias de Spring Data JPA, Jakarta Persistence y los paquetes internos de infraestructura y adaptadores.

**Decisión tomada:** Se seleccionó la segunda alternativa. La Regla 1 de ArchUnit prohíbe las dependencias de `procesamiento` hacia `infraestructura`, `adaptadores`, `org.springframework.data` y `jakarta.persistence`, pero permite utilizar anotaciones como `@Component`.

**Justificación:** Se decidió distinguir las dependencias porque `@Component` permite registrar automáticamente los validadores, calculadoras y notificadores en el contenedor de Spring, mientras que las clases de persistencia y acceso a datos introducirían responsabilidades de infraestructura dentro del procesamiento.

**Ventajas:**

- Mantiene separadas las responsabilidades de procesamiento y persistencia.
- Permite conservar la inyección automática de las estrategias y los observadores.
- Evita modificar las implementaciones existentes únicamente para satisfacer una restricción demasiado amplia.
- Facilita la verificación automática de límites arquitectónicos reales.
- Mantiene coherencia con la arquitectura en capas simples definida en ADR-001.

**Desventajas:**

- El paquete `procesamiento` conserva una dependencia de las anotaciones de Spring.
- Las clases no son completamente independientes del framework, aunque su lógica de negocio permanezca separada de la persistencia.
- Si en el futuro se requiere independencia total respecto a Spring, será necesario trasladar el registro de componentes a una configuración externa.

**Alternativa descartada:** Se descartó prohibir todas las importaciones de `org.springframework`, porque ello obligaría a eliminar las anotaciones `@Component` y modificar la configuración de inyección de dependencias, sin aportar un beneficio proporcional para los requisitos actuales.

**Relación con ADR-001:** Esta decisión es coherente con la arquitectura en capas simples seleccionada. El objetivo no es eliminar toda dependencia tecnológica, sino controlar aquellas que comprometerían las responsabilidades y los límites definidos para cada paquete.

**Conclusión:** Se considera adecuado permitir las anotaciones de estereotipo de Spring dentro de `procesamiento` y prohibir específicamente las dependencias de persistencia y de adaptadores. Esta decisión aplica los principios KISS y YAGNI, evitando restricciones innecesarias mientras protege los límites arquitectónicos relevantes del proyecto.

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