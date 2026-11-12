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

### Decisión de diseño 3 — Mantener arquitectura en capas simples y conservar el punto de extensión de notificaciones

**Problema identificado**
Después de integrar los patrones Chain of Responsibility, Strategy y Observer, fue necesario evaluar si el sistema debía adoptar una arquitectura Hexagonal completa, introduciendo puertos y adaptadores para todas las dependencias externas, o si convenía conservar la arquitectura en capas simples implementada durante la Parte 1. a evaluación se realizó considerando las necesidades actuales de persistencia y notificación, así como el criterio de la Guía de la Unidad 12, sección 6.2, relativo a sistemas con una vida útil prolongada y cambios de infraestructura previstos.

**Alternativas evaluadas**
1. **Migrar a arquitectura Hexagonal completa:** introducir una interfaz `RepositorioSolicitudes` como puerto de persistencia, crear un adaptador JPA que la implemente y establecer abstracciones adicionales para las integraciones externas.
2. **Mantener la arquitectura en capas simples:** conservar la dependencia directa de `SolicitudReembolsoService` hacia `RepositorioSolicitudesJpa` y aprovechar la interfaz `ObservadorCambioEstado` que ya desacopla los mecanismos de notificación.

**Decisión tomada**
Se seleccionó la segunda alternativa: mantener una arquitectura en capas simples para la persistencia y conservar el mecanismo Observer existente para las notificaciones.

**Justificación técnica**

En cuanto a la persistencia, actualmente se utiliza Spring Data JPA con H2 y no existe un requisito documentado que exija sustituir el mecanismo de almacenamiento o incorporar implementaciones alternativas. Crear un puerto adicional supondría introducir una abstracción sin una necesidad concreta que la justifique. or otra parte, el área de Contabilidad tiene prevista una integración con una API externa durante el próximo trimestre. Sin embargo, el servicio ya utiliza `List<ObservadorCambioEstado>` y no depende de las clases concretas de notificación.

Por esta razón, la futura integración podrá implementarse modificando o reemplazando `NotificadorContabilidad`, manteniendo el contrato `ObservadorCambioEstado` y sin necesidad de alterar la coordinación de los otros observadores. a decisión aplica los principios **YAGNI** y **KISS**, priorizando una arquitectura proporcional a las necesidades actuales y evitando introducir interfaces innecesarias.

**Consecuencias positivas**
- Se evita agregar interfaces y adaptadores de persistencia sin un requisito real.
- Se mantiene una estructura sencilla, comprensible y fácil de mantener.
- La futura integración externa de Contabilidad cuenta con un punto de extensión mediante Observer.
- Se conservan las responsabilidades diferenciadas de dominio, procesamiento, aplicación e infraestructura.
- Se reducen los costos asociados al mantenimiento de abstracciones especulativas.

**Consecuencias negativas**
- `SolicitudReembolsoService` permanece acoplado directamente a Spring Data JPA.
- Un cambio significativo en la estrategia de persistencia podría requerir introducir un puerto y refactorizar parte de la aplicación.
- La arquitectura no presenta simetría completa entre todas las dependencias externas.
- El mecanismo actual de notificación es síncrono y la futura integración con una API externa podría requerir manejo de fallos, reintentos, tiempos de espera e idempotencia.

**Condiciones para reconsiderar la decisión**
Se reevaluará la necesidad de adoptar puertos y adaptadores cuando existan requisitos concretos de persistencia alternativa, nuevas integraciones que no puedan aprovechar las interfaces existentes o necesidades verificables de aislamiento de infraestructura.

**Conclusión**
No se considera necesaria una migración completa a arquitectura Hexagonal. Se conserva la dependencia directa hacia JPA y se aprovecha el desacoplamiento que ya ofrece Observer para preparar la integración externa conocida. Esta elección evita sobreingeniería sin impedir una futura evolución del sistema.

---

## Resumen del ADR-001 — Decisión arquitectónica de persistencia y notificación

**Documento:** `docs/adr/ADR-001.md`  
**Estado:** Aceptado  
**Fecha:** 2026-11-11

El ADR-001 documenta la evaluación arquitectónica realizada después de integrar los tres patrones de comportamiento en el Sistema de Solicitudes de Reembolso de Gastos Corporativos.

La decisión establece dos criterios principales:

**1. Persistencia: conservar la arquitectura en capas simples.**
`SolicitudReembolsoService` mantiene su dependencia directa hacia `RepositorioSolicitudesJpa`, debido a que no existe actualmente una necesidad documentada de introducir una segunda implementación de persistencia. Por tanto, no se incorpora el puerto intermedio `RepositorioSolicitudes`.

**2. Notificaciones: conservar la interfaz Observer existente.**
La interfaz `ObservadorCambioEstado` ya permite separar la coordinación del servicio de las implementaciones de notificación. La integración futura con una API externa de Contabilidad podrá aprovechar este punto de extensión, sin introducir anticipadamente otra jerarquía de interfaces.

**Impacto arquitectónico**
La decisión permite conservar una estructura sencilla, mantener las responsabilidades diferenciadas y evitar abstracciones especulativas. Como contrapartida, se acepta conscientemente el acoplamiento del servicio con Spring Data JPA y la posibilidad de realizar ajustes futuros si cambian los requisitos de infraestructura. a decisión es coherente con los principios YAGNI y KISS y con las restricciones verificadas mediante ArchUnit.

**Relación con las reglas arquitectónicas**

- **Regla 1:** protege el paquete `procesamiento` frente a dependencias de infraestructura, adaptadores y tecnologías de persistencia.
- **Regla 2:** evita que el controlador REST acceda directamente a componentes fuera de los límites permitidos.
- **Regla 3:** verifica que las calculadoras concretas implementen el contrato `CalculadoraReembolso`.
- **Regla 4:** verifica que los notificadores concretos implementen `ObservadorCambioEstado`.

Estas reglas protegen los límites arquitectónicos definidos sin exigir un puerto de persistencia que fue descartado justificadamente.

**Resultado**
El ADR-001 establece que la complejidad arquitectónica debe responder a necesidades reales y verificables. No se descarta una futura evolución hacia una arquitectura más desacoplada, pero se evita implementarla anticipadamente sin evidencia que la justifique.
Para consultar el contexto completo, las alternativas evaluadas y las consecuencias positivas y negativas, revisar el archivo [ADR-001](docs/adr/ADR-001.md).

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

## Qué se descartó y por qué

### Análisis de impacto: alternativas descartadas por sobreingeniería

Durante el desarrollo del Sistema de Solicitudes de Reembolso de Gastos Corporativos se evaluaron alternativas que podrían aumentar la flexibilidad de la aplicación. Sin embargo, siguiendo el criterio de la Guía de la Unidad 12, sección 4.2, se consideró necesario distinguir entre una variabilidad real y documentada y una necesidad futura meramente especulativa. El objetivo de este análisis no consiste en rechazar definitivamente los patrones o las abstracciones adicionales, sino en justificar por qué no resultan necesarios para el alcance actual y establecer qué condiciones permitirían reconsiderarlos.

### 1. Uso de ApplicationEventPublisher y @EventListener para las notificaciones

**Alternativa considerada**
Se contempló utilizar `ApplicationEventPublisher` de Spring para publicar eventos de cambio de estado de las solicitudes y emplear métodos anotados con `@EventListener` para que los distintos interesados reaccionaran a esos eventos. Esta solución podría reemplazar el mecanismo actual, donde `SolicitudReembolsoService` recibe una lista de implementaciones de `ObservadorCambioEstado` mediante inyección de dependencias.

**Razones para descartarla**
Actualmente existen tres observadores concretos: `NotificadorEmpleado`, `NotificadorContabilidad` y `NotificadorAuditoria`. El servicio no depende directamente de estas clases, sino de la interfaz `ObservadorCambioEstado`. Por tanto, ya se consigue el desacoplamiento requerido para agregar o sustituir notificadores.

Incorporar un sistema de publicación de eventos mediante Spring introduciría un mecanismo adicional de comunicación sin resolver una necesidad funcional pendiente. Además, mantener una lista explícita de observadores facilita identificar el punto donde se generan las notificaciones y comprender el flujo de ejecución. La incorporación de eventos podría dificultar el seguimiento de las reacciones durante la depuración. Por otra parte, utilizar `ApplicationEventPublisher` no garantizaría automáticamente una ejecución asíncrona ni una entrega confiable de eventos.

**Decisión tomada**
Se conserva la interfaz `ObservadorCambioEstado` y la inyección de `List<ObservadorCambioEstado>`.

**Señal futura para reconsiderarla**
La decisión podría revisarse si diferentes módulos necesitaran publicar y consumir múltiples tipos de eventos de negocio, si surgiera la necesidad comprobada de desacoplar temporalmente las operaciones o si el sistema requiriera una infraestructura de eventos con requisitos específicos de entrega y procesamiento. En ese escenario se evaluaría si los eventos de Spring son suficientes o si se requiere un mecanismo de mensajería más robusto.

### 2. Implementación anticipada de Visitor o Abstract Factory para exportar reportes
**Alternativa considerada**
Se evaluó anticipar la generación de reportes de solicitudes en múltiples formatos, como CSV, PDF y Excel, mediante una jerarquía de exportadores basada en Visitor o Abstract Factory. Este diseño permitiría seleccionar implementaciones de exportación y facilitaría incorporar formatos adicionales en el futuro.

**Razones para descartarla**
La necesidad considerada actualmente se limita al formato CSV, sin solicitudes adicionales documentadas para PDF o Excel. Por tanto, no existe una variación real de formatos que justifique mantener distintas implementaciones y una estructura de abstracción adicional. Crear desde ahora interfaces, fábricas y exportadores de formatos no solicitados representaría un caso de sobreingeniería.

Además, una Abstract Factory estaría especialmente justificada si debieran crearse familias relacionadas de objetos, condición que no se ha identificado en este escenario. De acuerdo con el criterio de variabilidad de la sección 4.2 de la Guía, no se ha presentado una repetición suficiente de necesidades que justifique generalizar el diseño.

**Decisión tomada**
Se descarta implementar anticipadamente una jerarquía de exportadores. Si se desarrolla la exportación CSV, podrá resolverse inicialmente mediante un componente sencillo, sin introducir formatos adicionales que todavía no se requieren.

**Señal futura para reconsiderarla**
Se reconsiderará esta decisión cuando existan requisitos concretos para generar reportes en varios formatos, con comportamientos distintos que requieran implementaciones intercambiables. En ese momento deberá analizarse qué patrón se ajusta mejor al tipo de variabilidad identificada, en lugar de asumir de antemano que Visitor o Abstract Factory son necesarios.

### 3. Jerarquía de excepciones para cada validador — decisión propia del proyecto
**Alternativa considerada**
Durante la implementación de Chain of Responsibility se contempló utilizar excepciones especializadas para representar cada tipo de rechazo de una solicitud.

Por ejemplo:

- `DocumentosSoporteException`, cuando faltaran los documentos exigidos.
- `MontoMaximoException`, cuando se superara el límite permitido.
- `SolicitudDuplicadaException`, cuando existiera otra solicitud equivalente.

Cada excepción permitiría identificar directamente el tipo de incumplimiento mediante su clase.

**Razones para descartarla**
En la implementación actual, las validaciones producen un `ResultadoValidacion`, que expresa si la solicitud es válida y comunica el motivo de un rechazo. Cuando una validación falla, el servicio puede utilizar `SolicitudInvalidaException` para propagar ese motivo hacia el controlador REST. El manejo de errores existente no necesita realizar acciones diferentes según la clase de validador que rechazó la solicitud. En todos los casos se comunica una solicitud inválida mediante una respuesta HTTP 400.

Crear una clase de excepción por cada validación aumentaría el número de archivos y de tipos que deben mantenerse, sin que exista actualmente un comportamiento diferente que lo justifique. También podría generar una jerarquía creciente cada vez que se incorpore una nueva regla de validación. Por tanto, esa alternativa introduciría complejidad estructural sin resolver una necesidad funcional adicional.

**Decisión tomada**
Se conserva `ResultadoValidacion` como mecanismo para expresar los resultados de la cadena de validadores y `SolicitudInvalidaException` como excepción común de aplicación. La información específica sobre el rechazo se mantiene en el mensaje asociado a la validación.

**Señal futura para reconsiderarla**
La decisión podría reconsiderarse si diferentes tipos de rechazo requirieran tratamientos claramente diferenciados. Por ejemplo, si una solicitud duplicada necesitara un código de error propio, si los documentos faltantes activaran un proceso específico de subsanación o si clientes externos debieran identificar automáticamente el tipo de incumplimiento. En tal caso, antes de introducir una jerarquía completa de excepciones, también podría evaluarse utilizar códigos de error estructurados, evitando depender de la interpretación de mensajes de texto. Esta revisión estaría motivada por nuevos comportamientos documentados y no por una generalización anticipada.

### Conclusión del análisis de impacto

El análisis de las tres alternativas demuestra que una solución arquitectónica más sofisticada no siempre representa una mejora para el sistema. En las notificaciones, la interfaz `ObservadorCambioEstado` ya proporciona el desacoplamiento requerido. En los reportes, todavía no existe una variación de formatos que justifique una jerarquía de exportación. En las validaciones, el uso conjunto de `ResultadoValidacion` y `SolicitudInvalidaException` satisface el comportamiento actual sin requerir excepciones especializadas. Estas decisiones son coherentes con los principios YAGNI y KISS y con el criterio de introducir abstracciones cuando exista una variabilidad real. Las alternativas descartadas no se consideran incorrectas ni prohibidas. Permanecen como posibilidades de evolución que deberán evaluarse nuevamente cuando aparezcan requisitos verificables que justifiquen su incorporación. En consecuencia, el proyecto mantiene una arquitectura proporcional a su complejidad actual, evitando costos de mantenimiento innecesarios y conservando puntos de extensión donde ya existe una necesidad identificada.

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Data JPA
- H2 Database
- JUnit 5
- ArchUnit 1.x