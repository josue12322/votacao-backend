package com.global.votacao.infrastructure.client.cpfcnpj;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "cpfCnpjClient",
        url = "${votacao.elegibilidade.base-url}"
)
public interface CpfCnpjFeignClient {

    @GetMapping("/{token}/{codigo}/{documento}")
    CpfCnpjResponse consultarDocumento(
            @PathVariable("token") String token,
            @PathVariable("codigo") Integer codigo,
            @PathVariable("documento") String documento
    );
}


