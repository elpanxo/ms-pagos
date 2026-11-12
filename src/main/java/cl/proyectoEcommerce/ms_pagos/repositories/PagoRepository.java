package cl.proyectoEcommerce.ms_pagos.repositories;

import cl.proyectoEcommerce.ms_pagos.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByOrderId(Long orderId);
    Optional<Pago> findByWebpayToken(String token);
    Optional<Pago> findByBuyOrder(String buyOrder);
}
