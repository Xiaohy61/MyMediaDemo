//
// Created by skyward on 2022/1/24.
//

#ifndef MYMEDIADEMO_PLAYSTATUS_H
#define MYMEDIADEMO_PLAYSTATUS_H


class PlayStatus {

public:
    bool exit = false;
    bool seek = false;
    bool pause = false;
    bool load = true;
    PlayStatus();
};


#endif //MYMEDIADEMO_PLAYSTATUS_H
