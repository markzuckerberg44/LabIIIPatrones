package cl.ucn.pipefilter.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OrderIngressVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        
        vertx.setTimer(2000, id -> {
            JsonObject order1 = new JsonObject()
                .put("orderId", "ORD-001")
                .put("customerId", "CUST-123")
                .put("items", new JsonArray()
                    .add(new JsonObject()
                        .put("productId", "PROD-A")
                        .put("quantity", 2)
                        .put("unitPrice", 15000))
                    .add(new JsonObject()
                        .put("productId", "PROD-B")
                        .put("quantity", 1)
                        .put("unitPrice", 25000)))
                .put("couponCode", "DESCUENTO10")
                .put("currency", "CLP")
                .put("timestamp", "2025-11-20T12:34:56Z")
                .put("paymentMethod", "TARJETA_CREDITO");

            vertx.eventBus().send("order.raw", order1);
            System.out.println("[INGRESS] Orden enviada: " + order1.encodePrettily());

            JsonObject order2 = new JsonObject()
                .put("orderId", "ORD-002")
                .put("customerId", "CUST-456")
                .put("items", new JsonArray()
                    .add(new JsonObject()
                        .put("productId", "PROD-C")
                        .put("quantity", 2)
                        .put("unitPrice", 30000)))
                .put("couponCode", "DESCUENTO20")
                .put("currency", "CLP")
                .put("timestamp", "2025-11-20T13:00:00Z")
                .put("paymentMethod", "TARJETA_CREDITO");

            vertx.eventBus().send("order.raw", order2);
            System.out.println("[INGRESS] Orden enviada: " + order2.encodePrettily());
        });

        startPromise.complete();
    }
}
