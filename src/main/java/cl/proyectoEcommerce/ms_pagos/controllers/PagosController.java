package cl.proyectoEcommerce.ms_pagos.controllers;

import cl.proyectoEcommerce.ms_pagos.dtos.IniciarPagoRequest;
import cl.proyectoEcommerce.ms_pagos.dtos.IniciarPagoResponse;
import cl.proyectoEcommerce.ms_pagos.dtos.PagoResultadoDTO;
import cl.proyectoEcommerce.ms_pagos.services.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/api/pagos")
public class PagosController {
    private final PagoService pagoService;
    public PagosController(PagoService pagoService) { this.pagoService = pagoService; }

    @ResponseBody
    @PostMapping("/iniciar")
    public ResponseEntity<IniciarPagoResponse> iniciar(@Valid @RequestBody IniciarPagoRequest request,
            JwtAuthenticationToken authentication) {
        String clientId = authentication.getToken().getClaimAsString("oid");
        if (clientId == null || clientId.isBlank()) clientId = authentication.getToken().getSubject();
        String bearer = "Bearer " + authentication.getToken().getTokenValue();
        IniciarPagoResponse response = pagoService.iniciar(request.orderId(), clientId, bearer);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/retorno", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView retorno(@RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String tbkBuyOrder) {
        PagoResultadoDTO resultado;
        try {
            if (tbkToken != null && !tbkToken.isBlank()) {
                resultado = pagoService.cancelar(tbkBuyOrder);
            } else if (tokenWs != null && !tokenWs.isBlank()) {
                resultado = pagoService.confirmar(tokenWs);
            } else {
                resultado = new PagoResultadoDTO(null, "ERROR", null, null,
                        "Webpay no devolvió un token de transacción.");
            }
        } catch (RuntimeException ex) {
            resultado = new PagoResultadoDTO(null, "ERROR", null, null,
                    "No se pudo confirmar el resultado del pago. Contacta al comercio antes de volver a pagar.");
        }
        RedirectView redirect = new RedirectView(pagoService.urlResultado(resultado));
        redirect.setStatusCode(org.springframework.http.HttpStatus.SEE_OTHER);
        return redirect;
    }
}
