-- 单台开关柜季度电度量状况
CREATE TABLE MAXIMO.RE_VOLTAGE_EM_QUARTER
(
    REVOLTAGEEMQUARTERID   BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1 ),
    IEDNAME                VARGRAPHIC(100 CODEUNITS16),
    ASSETYPE               VARGRAPHIC(100 CODEUNITS16),
    PRODUCTMODEL           VARGRAPHIC(100 CODEUNITS16),
    LOCATION               VARGRAPHIC(100 CODEUNITS16),
    PRODUCTMODELC          VARGRAPHIC(100 CODEUNITS16),
    ActiveElectricDegree   DECIMAL(31, 4),
    ReactiveElectricDegree DECIMAL(31, 4),
    ColTime                VARGRAPHIC(50 CODEUNITS16),
    TIMEPOINT              VARGRAPHIC(50 CODEUNITS16),
    CREATETIME             VARGRAPHIC(50 CODEUNITS16),
    PRIMARY KEY (REVOLTAGEEMQUARTERID)
);


-- 单台开关柜年度电度量状况
CREATE TABLE MAXIMO.RE_VOLTAGE_EM_YEAR
(
    REVOLTAGEEMYEARID      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1 ),
    IEDNAME                VARGRAPHIC(100 CODEUNITS16),
    ASSETYPE               VARGRAPHIC(100 CODEUNITS16),
    PRODUCTMODEL           VARGRAPHIC(100 CODEUNITS16),
    LOCATION               VARGRAPHIC(100 CODEUNITS16),
    PRODUCTMODELC          VARGRAPHIC(100 CODEUNITS16),
    ActiveElectricDegree   DECIMAL(31, 4),
    ReactiveElectricDegree DECIMAL(31, 4),
    ColTime                VARGRAPHIC(50 CODEUNITS16),
    TIMEPOINT              VARGRAPHIC(50 CODEUNITS16),
    CREATETIME             VARGRAPHIC(50 CODEUNITS16),
    PRIMARY KEY (REVOLTAGEEMYEARID)
);
