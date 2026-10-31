package com.pharmacy.demo.service;

import java.time.LocalDate;

public interface CodeGenerator {

    String next(CodeKind kind);

    String preview(CodeKind kind);

    void raiseTo(CodeKind kind, LocalDate date, long value);
}
