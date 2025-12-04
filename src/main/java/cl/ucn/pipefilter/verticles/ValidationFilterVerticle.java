package cl.ucn.pipefilter.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ValidationFilterVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer("order.raw", message -> {
            JsonObject json = (JsonObject) message.body();
            
            if (validateOrder(json)) {
                System.out.println("[VALIDATION] Orden válida: " + json.getString("orderId"));
                vertx.eventBus().send("order.validated", json);
            } else {
                System.err.println("[VALIDATION] Orden inválida: " + json.getString("orderId"));
                vertx.eventBus().send("order.error", json);
            }
        });
        startPromise.complete();
    }

    private boolean validateOrder(JsonObject json) {
        if (isNullOrEmpty(json.getString("orderId"))) return false;
        if (isNullOrEmpty(json.getString("customerId"))) return false;
        if (isNullOrEmpty(json.getString("currency"))) return false;
        if (isNullOrEmpty(json.getString("paymentMethod"))) return false;
        if (isNullOrEmpty(json.getString("timestamp"))) return false;

        JsonArray items = json.getJsonArray("items");
        if (items == null || items.isEmpty()) return false;

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.getJsonObject(i);
            if (isNullOrEmpty(item.getString("productId"))) return false;
            
            Integer quantity = item.getInteger("quantity");
            if (quantity == null || quantity <= 0) return false;
            
            Long unitPrice = item.getLong("unitPrice");
            if (unitPrice == null || unitPrice < 0) return false;
        }

        return true;
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
