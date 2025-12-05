package cl.ucn.pipefilter.verticles;

import cl.ucn.pipefilter.config.JPAUtil;
import cl.ucn.pipefilter.model.Order;
import cl.ucn.pipefilter.model.OrderItem;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.persistence.EntityManager;

import java.time.Instant;

public class PersistenceFilterVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {

        // Este consumidor escucha las órdenes ya procesadas (pricing + fraude)
        vertx.eventBus().consumer("order.persist", message -> {
            JsonObject json = (JsonObject) message.body();

            vertx.executeBlocking(promise -> {
                try {
                    persistOrder(json);
                    promise.complete();
                } catch (Exception e) {
                    promise.fail(e);
                }
            }, res -> {
                if (res.succeeded()) {
                    System.out.println("[PERSIST] Orden almacenada: " + json.getString("orderId"));
                } else {
                    System.err.println("[PERSIST] Error al almacenar orden: " + res.cause());
                }
            });
        });
        startPromise.complete();
    }
    

    private void persistOrder(JsonObject json) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            String orderId = json.getString("orderId");

            // 1. INTENTAR CARGAR LA ORDEN EXISTENTE
            Order order = em.find(Order.class, orderId);

            // Si la orden no existe, la creamos
            if (order == null) {
                order = new Order();
                order.setOrderId(orderId);
            }

            // 2. ACTUALIZAR TODOS LOS CAMPOS
            order.setCustomerId(json.getString("customerId"));
            order.setCurrency(json.getString("currency"));
            order.setPaymentMethod(json.getString("paymentMethod"));
            order.setSubtotal(json.getLong("subtotal"));
            order.setDiscount(json.getLong("discount"));
            order.setTotal(json.getLong("total"));
            order.setStatus(json.getString("status"));

            String ts = json.getString("timestamp");
            if (ts != null) {
                order.setTimestamp(Instant.parse(ts));
            }

            // 3. ACTUALIZAR ITEMS

            // Es crucial limpiar los items antiguos si estás re-procesando
            // Esto depende del orphanRemoval=true y CascadeType.ALL
            order.getItems().clear();

            JsonArray itemsArray = json.getJsonArray("items");
            if (itemsArray != null) {
                Order finalOrder = order;
                itemsArray.forEach(obj -> {
                    JsonObject itemJson = (JsonObject) obj;
                    OrderItem item = new OrderItem();
                    item.setProductId(itemJson.getString("productId"));
                    item.setQuantity(itemJson.getInteger("quantity"));
                    item.setUnitPrice(itemJson.getLong("unitPrice"));

                    // Mapeo bidireccional
                    item.setOrder(finalOrder);
                    finalOrder.getItems().add(item);
                });
            }

            // 4. USAR MERGE (ya sea nueva o existente)
            em.merge(order);
            em.getTransaction().commit();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error en persistencia para orden " + json.getString("orderId"), e);
        } finally {
            em.close();
        }
    }
}
