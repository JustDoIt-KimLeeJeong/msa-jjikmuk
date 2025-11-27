package com.jjikmuk.execution_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "symbol_seq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SymbolSeqEntity {

    @Id
    @Column(length = 20)
    private String symbol;

    private long sequence;

    public SymbolSeqEntity(String symbol) {
        this.symbol = symbol;
        this.sequence = 0L;
    }

    public void increase() {
        this.sequence++;
    }
}