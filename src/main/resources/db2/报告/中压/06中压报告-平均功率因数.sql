-- 开关柜平均功率因数
CREATE TABLE MAXIMO.RE_VOLTAGE_POWERFACTOR
(
    REVOLTAGEPOWERFACTORID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1 ),
    ASSETYPE               VARGRAPHIC(100 CODEUNITS16),
    PRODUCTMODEL           VARGRAPHIC(100 CODEUNITS16),
    LOCATION               VARGRAPHIC(100 CODEUNITS16),
    PRODUCTMODELC          VARGRAPHIC(100 CODEUNITS16),
    POWERFACTOR            DECIMAL(8, 4),
    TIMEPOINT              VARGRAPHIC(50 CODEUNITS16),
    CREATETIME             VARGRAPHIC(50 CODEUNITS16),
    PRIMARY KEY (REVOLTAGEPOWERFACTORID)
);

