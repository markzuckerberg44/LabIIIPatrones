package cl.ucn.pipefilter.main;

import cl.ucn.pipefilter.verticles.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class MainVerticle extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start() {

        // Fuente de datos: genera o recibe órdenes
        vertx.deployVerticle(new OrderIngressVerticle());

        // Filtro 1: Validación
        vertx.deployVerticle(new ValidationFilterVerticle());

        // Filtro 2: Cálculo de subtotal, descuentos y total
        vertx.deployVerticle(new PricingFilterVerticle());

        // Filtro 3: Reglas de fraude / revisión
        vertx.deployVerticle(new FraudCheckFilterVerticle());

        // Filtro 4: Persistencia en base de datos con JPA/Hibernate
        // IMPORTANTE: este es un worker verticle
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
