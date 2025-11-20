package cl.ucn.pipefilter.config;

import cl.ucn.pipefilter.model.Order;
import cl.ucn.pipefilter.model.OrderItem;

import jakarta.persistence.EntityManager;
import java.util.List;

public class OrderPrinter {

    public static void printAllOrders() {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            em.getTransaction().begin();

            List<Order> orders = em
                    .createQuery("SELECT o FROM Order o", Order.class)
                    .getResultList();

            System.out.println("===== ÓRDENES EN LA BASE DE DATOS =====");
            for (Order o : orders) {
                System.out.println("Order ID     : " + o.getOrderId());
                System.out.println("Cliente      : " + o.getCustomerId());
                System.out.println("Fecha        : " + o.getTimestamp());
                System.out.println("Moneda       : " + o.getCurrency());
                System.out.println("Medio pago   : " + o.getPaymentMethod());
                System.out.println("Subtotal     : " + o.getSubtotal());
                System.out.println("Descuento    : " + o.getDiscount());
                System.out.println("Total        : " + o.getTotal());
                System.out.println("Estado       : " + o.getStatus());
                System.out.println("Items:");

                for (OrderItem item : o.getItems()) {
                    System.out.println("  - Producto : " + item.getProductId());
                    System.out.println("    Cantidad: " + item.getQuantity());
                    System.out.println("    Precio  : " + item.getUnitPrice());
                }
                System.out.println("----------------------------------------");
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}

