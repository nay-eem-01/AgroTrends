package com.project.agriculturalblogapplication.payloads;

import com.project.agriculturalblogapplication.enums.AscOrDescType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationArgs {

    private int pageNo;

    private int pageSize;

    private String sortBy;

    private AscOrDescType ascOrDesc;
}
