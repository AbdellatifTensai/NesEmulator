#pragma once

#include <cstdint>
#include <vector>

namespace NES{

    struct CPU{
        uint8_t register_A;
        uint8_t register_X;
        uint8_t status;
        uint16_t program_counter;
        uint8_t memory[0xFFFF];

        CPU();
        void load(std::vector<uint8_t> program);
        void run();
        void printState();

        private:
        uint8_t memoryRead(uint16_t addr);
        void memoryWrite(uint16_t addr, uint8_t data);
        void updateZNFlags(uint8_t cpu_register);
        void LDA(uint8_t val);
        void TAX();
        void INX();
    };
    
}