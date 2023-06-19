#include <stdio.h>
#include "CPU.h"

int main(){

    std::vector<uint8_t> program = {0xA9, 0xFE, 0xAA, 0xE8, 0x00};

    NES::CPU cpu;
    cpu.load(program);
    cpu.run();

    cpu.printState();
    return 0;
}
