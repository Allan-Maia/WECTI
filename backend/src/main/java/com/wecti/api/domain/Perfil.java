package com.wecti.api.domain;

// So ADMIN e ALUNO - confirmado com o stakeholder do projeto que esta
// versao nao precisa de um perfil PROFESSOR separado (o admin cadastra
// eventos e usuarios, o aluno se autocadastra e participa). Nao
// reintroduzir sem confirmar de novo.
public enum Perfil {
    ADMIN,
    ALUNO
}
