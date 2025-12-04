package cl.ucn.pipefilter.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import cl.ucn.pipefilter.config.OrderPrinter;

public class OrderPrinterVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.setTimer(5000, id -> {
            vertx.executeBlocking(promise -> {
                OrderPrinter.printAllOrders();
                promise.complete();
            }, res -> {
                System.out.println("Impresión de órdenes finalizada.");
            });
        });
        startPromise.complete();
    }
}
