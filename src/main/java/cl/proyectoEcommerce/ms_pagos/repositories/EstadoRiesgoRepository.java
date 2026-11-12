package cl.proyectoEcommerce.ms_pagos.repositories;

import cl.proyectoEcommerce.ms_pagos.entities.EstadoRiesgoCliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface EstadoRiesgoRepository extends JpaRepository<EstadoRiesgoCliente, String> {
    @Modifying
    @Query(value = "INSERT INTO estado_riesgo_pago (client_id, updated_at) VALUES (:clientId, CURRENT_TIMESTAMP) ON CONFLICT (client_id) DO NOTHING", nativeQuery = true)
    void createIfMissing(@Param("clientId") String clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstadoRiesgoCliente e where e.clientId = :clientId")
    Optional<EstadoRiesgoCliente> lockByClientId(@Param("clientId") String clientId);
}
