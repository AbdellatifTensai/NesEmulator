#include "CPU.h"
#include <stdio.h>
#include <cstring>

#define FLAG_N 0b10000000
#define FLAG_Z 0b00000010

namespace NES{

    CPU::CPU(){
        program_counter = 0;
        status = 0; //NV-BDIZC
        register_A = 0;
        register_X = 0;
    }

    void CPU::load(std::vector<uint8_t> program){
        memcpy(&memory[0x8000], &program[0], program.size()); 
        program_counter = 0x8000;
    }

    void CPU::run(){

        for(;;){
            uint8_t opcode = memoryRead(program_counter);
            program_counter++;
            printf("program counter: %X\nopcode: %X\n",program_counter, opcode);

            switch(opcode){
                case 0xA9:{
                    uint8_t parameter = memory[program_counter];
                    program_counter++;
                    LDA(parameter);
                    break;
                }

                case 0xAA: TAX(); break;

                case 0xE8: INX(); break;

                case 0x00: return;

                default: break;
            }
        }
    }

    uint8_t CPU::memoryRead(uint16_t addr){
        return memory[addr];
    }

    void CPU::memoryWrite(uint16_t addr, uint8_t data){
        memory[addr] = data;
    }

    void CPU::LDA(uint8_t val){
        register_A = val;
        updateZNFlags(register_A);
    }

    void CPU::TAX(){
        register_X = register_A;
        updateZNFlags(register_X);
    }

    void CPU::INX(){
        register_X++;
        updateZNFlags(register_X);
    }

    void CPU::updateZNFlags(uint8_t cpu_register){
        status = cpu_register == 0 ? status | FLAG_Z : status & ~FLAG_Z;
        status = (cpu_register & FLAG_N) != 0 ? status | FLAG_N : status & ~FLAG_N;
    } 

    void CPU::printState(){
        printf(
            "Register A: %X\n"
            "Register X: %X\n"
            "Program Counter: %X\n"
            "Status Flags: 0x%X\n",
            register_A,
            register_X,
            program_counter,
            status
        );
    }
}