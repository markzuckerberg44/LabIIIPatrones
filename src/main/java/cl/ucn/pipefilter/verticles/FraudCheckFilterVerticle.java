package cl.ucn.pipefilter.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FraudCheckFilterVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer("order.priced", message -> {
            JsonObject json = (JsonObject) message.body();
            
            long total = json.getLong("total");
            String paymentMethod = json.getString("paymentMethod");
            JsonArray items = json.getJsonArray("items");

            if (total > 200000 && "TARJETA_CREDITO".equals(paymentMethod)) {
                json.put("status", "REVISION");
                System.out.println("[FRAUD] Orden sospechosa (monto alto): " + json.getString("orderId"));
            } else if (items.size() > 20) {
                json.put("status", "REVISION");
                System.out.println("[FRAUD] Orden sospechosa (muchos productos): " + json.getString("orderId"));
            } else {
                System.out.println("[FRAUD] Orden aprobada: " + json.getString("orderId"));
            }

            vertx.eventBus().send("order.persist", json);
        });
        startPromise.complete();
    }
}
