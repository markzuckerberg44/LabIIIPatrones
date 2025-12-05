# Laboratorio III: Arquitectura Pipe & Filter <br> Profesor: Daniel San Martín <br> Patrones de Software y Programación.
Mauricio Muñoz 21542213-5 / Julian Honores 21328088-0

---

## 🎯 Objetivo del Laboratorio
Implementar un pipeline de procesamiento de órdenes de compra usando:
- El patrón **Pipe & Filter**
- **Vert.x** y su EventBus
- Mensajes **JSON**
- Persistencia en base de datos mediante **JPA/Hibernate**

El patrón Pipe & Filter es un estilo arquitectural donde una tarea compleja se divide en múltiples pasos independientes, llamados filtros, conectados entre sí mediante tuberías (pipes).

🔹 ¿Qué es un filtro?

- Un filtro es un componente que:

        1. Recibe datos de entrada
        2. Los transforma, valida o procesa
        3. Produce datos de salida (o descarta)

  Cada filtro realiza una tarea específica y autónoma.

🔹 ¿Qué es un pipe?

    Un pipe es el canal que conecta un filtro con el siguiente. 
    Transmiten el resultado de un filtro como entrada del siguiente.

- Permite:

        1. Procesar datos en etapas
        2. Aislar responsabilidades
        3. Reusar filtros
        4. Reemplazar filtros sin romper el sistema
        5. Encadenar pasos fácilmente

Cada etapa del pipeline será un **Verticle**, y cada enlace un **canal del EventBus**.

---

## 🛒 Contexto
La universidad administra una tienda online paa vender productos institucionales tales como libros, poleras de clas carreras
tazas con logo, etc. Cada compra genera un **JSON de orden**, pero estos datos pueden venir incompletos, con errores o con montos sospechosos.

Su tarea es implementar un sistema modular basado en filtros encadenados.

---

## 🔧 Arquitectura Pipe & Filter

OrderIngress → ValidationFilter → PricingFilter → FraudCheckFilter → PersistenceFilter

---

## 📄 Formato JSON de Entrada

``` json
{
    "orderId": "...",
    "customerId": "...",
    "items": [
                { "productId": "...", 
                  "quantity": 2, 
                  "unitPrice": 15000 
                }
    ],
    "couponCode": "DESCUENTO10",
    "currency": "CLP",
    "timestamp": "2025-11-20T12:34:56Z",
    "paymentMethod": "CREDIT_CARD"
}
```
---

## ✔ Filtros a implementar

## 🧩 Filtros del Pipeline (definidos de forma concreta)

---

### ✅ 1. ValidationFilter (Filtro de Validación)

**Entrada:** JSON desde `order.raw`  
**Salida OK:** `order.validated`  
**Salida error:** `order.error`

**Reglas:**

1. **Campos obligatorios:**
  - `orderId` (String, no vacío)
  - `customerId` (String)
  - `items` (array, no vacío)
  - `currency` (String)
  - `paymentMethod` (String)
  - `timestamp` (String, formato ISO 8601)

2. **Reglas para cada item:**
  - `productId` no vacío
  - `quantity` entero **> 0**
  - `unitPrice` entero **≥ 0**

3. **Si alguna regla falla:**
  - Se descarta.

4. **Si pasa todas las validaciones:**
  - Se envía el mismo JSON a `order.validated`.

---

### ✅ 2. PricingFilter (Filtro de Precios y Totales)

**Entrada:** JSON desde `order.validated`  
**Salida:** `order.priced`

**Reglas:**

1. **Cálculo del subtotal:**
  - `subtotal = Σ (quantity * unitPrice)` para todos los ítems.

2. **Descuentos según cupón:**
  - `DESCUENTO10` → 10% del subtotal
  - `DESCUENTO20` → 20% si el subtotal ≥ 50.000
  - Otro caso: descuento = 0

3. **Cálculo del total:**
  - `total = subtotal - discount`

4. **Agregar/modificar campos en el JSON:**
  - `subtotal`
  - `discount`
  - `total`
  - `status = "CALCULADA"`

5. **Enviar el JSON a `order.priced`.**

---

### ✅ 3. FraudCheckFilter (Filtro de Fraude / Revisión)

**Entrada:** JSON desde `order.priced`  
**Salida:** `order.persist`

**Reglas:**

1. **Monto alto con tarjeta de crédito:**
  - Si `total > 200000` **y** `paymentMethod = "TARJETA_CREDITO"`  
    → marcar orden como sospechosa (`status = "REVISION"`)

2. **Demasiados productos:**
  - Si `items.length > 20`  
    → `status = "REVISION"`

3. **Si no hay señales de fraude:**
  - Mantener `status = "CALCULADA"`

4. **En todos los casos:**
  - Enviar el JSON resultante a `order.persist`.

---

## 🗄 Entidades JPA

### Order
- orderId
- customerId
- timestamp
- currency
- paymentMethod
- subtotal
- discount
- total
- status

### OrderItem
- id
- productId
- quantity
- unitPrice

---

## Implementación

### Estructura del proyecto

El proyecto implementa 6 verticles que conforman el pipeline completo:

- OrderIngressVerticle: Genera órdenes de prueba y las envía al canal order.raw
- ValidationFilterVerticle: Valida campos obligatorios y reglas de negocio
- PricingFilterVerticle: Calcula subtotal, aplica descuentos según cupones y calcula total
- FraudCheckFilterVerticle: Detecta órdenes sospechosas por monto o cantidad de items
- PersistenceFilterVerticle: Persiste las órdenes en base de datos SQLite usando JPA
- OrderPrinterVerticle: Lee e imprime todas las órdenes almacenadas

### Canales EventBus utilizados

- order.raw: Órdenes sin validar
- order.validated: Órdenes validadas
- order.priced: Órdenes con precios calculados
- order.persist: Órdenes listas para persistir
- order.error: Órdenes rechazadas por validación

### Problema encontrado durante el desarrollo

Durante las pruebas se detectó que los items de las órdenes no aparecían en la salida, aunque las órdenes sí se guardaban en la base de datos.

Análisis del problema:
- Se verificó mediante consultas SQL directas que los items sí estaban guardados físicamente
- Al revisar la tabla order_items se descubrió que algunos registros tenían id con valor NULL
- La tabla fue creada sin la propiedad AUTOINCREMENT en SQLite

Causa raíz:
- JPA requiere que todas las entidades tengan un valor válido en su campo @Id
- SQLite no genera IDs automáticamente si la tabla no tiene AUTOINCREMENT
- Hibernate ignora filas con id NULL porque no puede instanciar entidades sin clave primaria

Solución aplicada:
- Se recreó la tabla order_items con INTEGER PRIMARY KEY AUTOINCREMENT
- Se preservaron las órdenes existentes con IDs válidos
- Se eliminaron las órdenes con items sin ID
- Se modificó OrderPrinter para usar JOIN FETCH y cargar items eagerly

Resultado:
- Los nuevos items se guardan con IDs autogenerados correctamente
- Las órdenes muestran todos sus items al consultar la base de datos
- El pipeline completo funciona según las especificaciones

---
