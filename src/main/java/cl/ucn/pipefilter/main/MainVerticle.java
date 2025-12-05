package cl.ucn.pipefilter.main;

import cl.ucn.pipefilter.verticles.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class MainVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
        
        try {
            System.out.println("Presiona Enter para salir...");
            System.in.read();
            vertx.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {

        vertx.deployVerticle(new OrderIngressVerticle());
        vertx.deployVerticle(new ValidationFilterVerticle());
        vertx.deployVerticle(new PricingFilterVerticle());
        vertx.deployVerticle(new FraudCheckFilterVerticle());
        vertx.deployVerticle(new PersistenceFilterVerticle(),
                event -> {
                    if (event.succeeded()) {
                        System.out.println("PersistenceFilterVerticle desplegado correctamente.");
                    } else {
                        System.err.println("Error desplegando PersistenceFilterVerticle: " + event.cause());
                    }
                });
        vertx.deployVerticle(new OrderPrinterVerticle());
    }
}
