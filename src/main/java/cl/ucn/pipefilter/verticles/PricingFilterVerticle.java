package cl.ucn.pipefilter.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PricingFilterVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.eventBus().consumer("order.validated", message -> {
            JsonObject json = (JsonObject) message.body();
            
            long subtotal = calculateSubtotal(json);
            long discount = calculateDiscount(json, subtotal);
            long total = subtotal - discount;

            json.put("subtotal", subtotal);
            json.put("discount", discount);
            json.put("total", total);
            json.put("status", "CALCULADA");

            System.out.println("[PRICING] Orden procesada: " + json.getString("orderId") + 
                             " | Subtotal: " + subtotal + " | Descuento: " + discount + " | Total: " + total);

            vertx.eventBus().send("order.priced", json);
        });
        startPromise.complete();
    }

    private long calculateSubtotal(JsonObject json) {
        JsonArray items = json.getJsonArray("items");
        long subtotal = 0;
        
        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.getJsonObject(i);
            int quantity = item.getInteger("quantity");
            long unitPrice = item.getLong("unitPrice");
            subtotal += quantity * unitPrice;
        }
        
        return subtotal;
    }

    private long calculateDiscount(JsonObject json, long subtotal) {
        String couponCode = json.getString("couponCode", "");
        
        switch (couponCode) {
            case "DESCUENTO10":
                return (long) (subtotal * 0.10);
            case "DESCUENTO20":
                if (subtotal >= 50000) {
                    return (long) (subtotal * 0.20);
                }
                return 0;
            default:
                return 0;
        }
    }
}
