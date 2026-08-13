package com.wecti.api.exception;

import com.wecti.api.dto.ErroResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(404, "recurso_nao_encontrado", ex.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraNegocio(RegraNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErroResponse.de(422, "regra_de_negocio", ex.getMessage()));
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> handleConflito(ConflitoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(409, "conflito", ex.getMessage()));
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ErroResponse> handleCredenciaisInvalidas(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErroResponse.de(401, "credenciais_invalidas", "Email ou senha invalidos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResponse.de(403, "acesso_negado", "Perfil sem permissao para esta operacao"));
    }

    @ExceptionHandler(CampoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleCampoInvalido(CampoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(400, "campo_invalido", ex.getMessage(), ex.getCampo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        var erro = ex.getBindingResult().getFieldErrors().stream().findFirst();
        String campo = erro.map(e -> e.getField()).orElse(null);
        String mensagem = erro.map(e -> e.getDefaultMessage()).orElse("Dados invalidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(400, "campo_invalido", mensagem, campo));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(400, "campo_invalido", ex.getMessage()));
    }

    /** JSON malformado, enum/UUID invalido no corpo da requisicao, etc. -
     *  sem isso, cai no tratamento padrao do Spring (fora do nosso
     *  formato de resposta), mas ainda assim como 400, nao como 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(400, "campo_invalido", "Corpo da requisicao invalido ou malformado"));
    }

    /**
     * Rede de seguranca final: qualquer excecao nao mapeada explicitamente
     * (bug, falha de I/O, etc.) vira um 500 controlado no nosso formato,
     * em vez de vazar a pagina de erro padrao do Spring (ou pior, um
     * stack trace) para o cliente. O detalhe do erro vai só pro log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex) {
        log.error("Erro interno nao tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.de(500, "erro_interno", "Ocorreu um erro interno. Tente novamente mais tarde."));
    }
}
