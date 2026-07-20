package com.global.votacao.infrastructure.client.cpfcnpj;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CpfCnpjAssociadoElegibilidadeClientTest {

    @Mock
    private CpfCnpjFeignClient cpfCnpjFeignClient;

    private CpfCnpjAssociadoElegibilidadeClient client;

    @BeforeEach
    void setUp() {
        client = new CpfCnpjAssociadoElegibilidadeClient(
                new AssociadoElegibilidadeProperties(true, "https://api.cpfcnpj.com.br", "token-teste", 9, 6),
                cpfCnpjFeignClient,
                new CpfCnpjErrorHandler(new ObjectMapper())
        );
    }

    @Test
    void deveValidarCpfComSucesso() {
        when(cpfCnpjFeignClient.consultarDocumento("token-teste", 9, "61500421381"))
                .thenReturn(CpfCnpjResponse.builder().status(1).cpf("615.004.213-81").nome("Test Token").build());

        client.validarElegibilidade("615.004.213-81", TipoDocumento.CPF);

        verify(cpfCnpjFeignClient).consultarDocumento("token-teste", 9, "61500421381");
    }

    @Test
    void deveValidarCnpjComSucesso() {
        when(cpfCnpjFeignClient.consultarDocumento("token-teste", 6, "11222333000181"))
                .thenReturn(CpfCnpjResponse.builder().status(1).cnpj("11.222.333/0001-81").nome("Empresa Teste").build());

        client.validarElegibilidade("11.222.333/0001-81", TipoDocumento.CNPJ);

        verify(cpfCnpjFeignClient).consultarDocumento("token-teste", 6, "11222333000181");
    }

    @Test
    void deveIgnorarValidacaoQuandoElegibilidadeEstiverDesabilitada() {
        CpfCnpjAssociadoElegibilidadeClient clientDesabilitado = new CpfCnpjAssociadoElegibilidadeClient(
                new AssociadoElegibilidadeProperties(false, "https://api.cpfcnpj.com.br", "token-teste", 9, 6),
                cpfCnpjFeignClient,
                new CpfCnpjErrorHandler(new ObjectMapper())
        );

        clientDesabilitado.validarElegibilidade("documento-invalido", null);

        verifyNoInteractions(cpfCnpjFeignClient);
    }

    @Test
    void deveBloquearQuandoTipoDocumentoForNulo() {
        assertThatThrownBy(() -> client.validarElegibilidade("61500421381", null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Tipo");
    }

    @Test
    void deveBloquearCpfComTamanhoInvalido() {
        assertThatThrownBy(() -> client.validarElegibilidade("123", TipoDocumento.CPF))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("11");
        verifyNoInteractions(cpfCnpjFeignClient);
    }

    @Test
    void deveBloquearCnpjComTamanhoInvalido() {
        assertThatThrownBy(() -> client.validarElegibilidade("123", TipoDocumento.CNPJ))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("14");
        verifyNoInteractions(cpfCnpjFeignClient);
    }

    @Test
    void deveBloquearQuandoApiRetornarStatusZero() {
        when(cpfCnpjFeignClient.consultarDocumento("token-teste", 9, "61500421381"))
                .thenReturn(CpfCnpjResponse.builder().status(0).erro("CPF inválido!").build());

        assertThatThrownBy(() -> client.validarElegibilidade("61500421381", TipoDocumento.CPF))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CPF inválido");
    }
}
