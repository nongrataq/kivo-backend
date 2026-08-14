package com.kivo.backend.sharing.application;

public interface QrCodeGenerator {

  byte[] generatePng(String content, int size);
}
