package com.wecti.api.repository;

import com.wecti.api.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    List<Evento> findByDataHoraInicioAfter(LocalDateTime momento);
    List<Evento> findByDataHoraFimBefore(LocalDateTime momento);

    /**
     * Trava a linha do evento ate o fim da transacao.
     *
     * <p><b>Por que travar.</b> "Contar inscritos e depois inserir" nao e
     * atomico: com a turma inteira clicando em inscrever ao mesmo tempo,
     * dois alunos podem contar 399 de 400 no mesmo instante e ambos
     * entrarem, estourando a capacidade. E o mesmo tipo de corrida que a
     * constraint de unicidade resolve no check-in - aqui nao da para usar
     * constraint, porque o limite e um numero por evento, e nao uma
     * duplicata.
     *
     * <p><b>Por que SQL nativo.</b> Com {@code @Lock(PESSIMISTIC_WRITE)}
     * o Hibernate gera {@code FOR UPDATE OF <alias>}. O MySQL 8 aceita
     * essa forma, mas o <b>MariaDB nao</b> - e hospedagem compartilhada
     * costuma servir MariaDB no lugar do MySQL sem avisar. O erro so
     * apareceria em producao, na primeira inscricao. {@code FOR UPDATE}
     * puro funciona nos dois.
     *
     * <p>O id vai como String porque a coluna e VARCHAR(36) (ver
     * V1__init.sql): em consulta nativa nao ha conversao automatica de
     * UUID para o formato gravado.
     */
    @Query(value = "SELECT * FROM eventos WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Evento> travarParaInscricao(@Param("id") String id);
}
