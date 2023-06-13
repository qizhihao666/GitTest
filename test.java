package com.glodon.gys.inspection.helper;


import java.util.Collections;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by zhaoq-m.
 * Date: 2018/2/26
 */
public class BillDbHelper {

    /**
     * 查询磅单AI信息，并放入材料中
     *
     * @param materialList
     * @param billId
     * @param tablePref
     */
    private void getMaterialAiInfo(List<BillMaterialInfoObsolete> materialList,
                                   Long billId,
                                   String tablePref) {
        List<BillMaterialAiInfoObsolete> billMaterialAiInfos = billDbMapper.queryBillAiInfo(tablePref, billId);
        if (CollectionUtils.isEmpty(billMaterialAiInfos)) {
            return;
        }
        List<BillMaterialAiRecordInfoObsolete> aiRecordInfoList = billDbMapper.queryBillAiRecord(tablePref, billId);
        if (CollectionUtils.isEmpty(aiRecordInfoList)) {
            return;
        }

        Map<Long, BillMaterialAiInfoObsolete> billMaterialAiInfoMap
                = billMaterialAiInfos.stream().collect(Collectors.toMap(BillMaterialAiInfoObsolete::getBillMaterialId, (p) -> p));
        Map<Long, List<BillMaterialAiRecordInfoObsolete>> aiRecordMap
                = aiRecordInfoList.stream().collect(Collectors.groupingBy(BillMaterialAiRecordInfoObsolete::getBillMaterialId));
        for (BillMaterialInfoObsolete material : materialList) {
            Long billMaterialId = material.getId();
            if (billMaterialAiInfoMap.containsKey(billMaterialId)) {
                BillMaterialAiInfoObsolete billMaterialInfoAi = billMaterialAiInfoMap.get(billMaterialId);
                material.setBillMaterialAiInfo(billMaterialInfoAi);
            }
            if (aiRecordMap.containsKey(billMaterialId)) {
                List<BillMaterialAiRecordInfoObsolete> aiRecordList = aiRecordMap.get(billMaterialId);
                material.setAiRecordInfoList(aiRecordList);
            }
        }
    }

    public BillInfoObsolete queryBillByGuid(String guid) {

        String tablePref = InspectionConstants.getTablePref(false, BillTypeEnum.RECEIVE.getBillType());
        BillInfoObsolete obsolete = billDbMapper.queryBillByGuid(guid, tablePref);
        if (Objects.nonNull(obsolete)) {
            return obsolete;
        }
        tablePref = InspectionConstants.getTablePref(false, BillTypeEnum.PAYOUT.getBillType());
        obsolete = billDbMapper.queryBillByGuid(guid, tablePref);
        return obsolete;
    }
}
