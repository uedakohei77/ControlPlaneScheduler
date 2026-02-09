package com.example.demo;

import java.util.List;

public interface OutputFormatter {
    void print(List<ScheduleBucket> schedule);
    void setCapacity(int capacity);
}
