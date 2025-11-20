package cl.ucn.pipefilter.verticles;

import io.vertx.core.AbstractVerticle;
import cl.ucn.pipefilter.config.OrderPrinter;


public class OrderPrinterVerticle extends AbstractVerticle {

    @Override
    public void start() {
        vertx.executeBlocking(promise -> {
            OrderPrinter.printAllOrders();
            promise.complete();
        }, res -> {
            System.out.println("Impresión de órdenes finalizada.");
        });
    }
}
