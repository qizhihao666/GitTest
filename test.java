package com.glodon.gys.inspection.helper;

import com.glodon.gys.inspection.common.constants.BizCodeEnum;
import com.glodon.gys.inspection.common.constants.InspectionConstants;
import com.glodon.gys.inspection.common.entity.BizFlowRelation;
import com.glodon.gys.inspection.common.entity.obsolete.*;
import com.glodon.gys.inspection.common.enumerable.BillTypeEnum;
import com.glodon.gys.inspection.mapper.inspection.bill.BizFlowRelationMapper;
import com.glodon.gys.inspection.mapper.inventory.BillDbMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by zhaoq-m.
 * Date: 2018/2/26
 */
@Slf4j
@Component
public class BillDbHelper {
    @Autowired
    private BillDbMapper billDbMapper;
    aaaa；
    /**
     * 磅单引用关系
     */
    @Autowired
    private BizFlowRelationMapper bizFlowRelationMapper;

    /**
     * 根据磅单ID和磅单类型查询【未作废】全磅单信息
     *
     * @param billId   磅单ID
     * @param billType 磅单类型
     * @return
     */
    public BillInfoObsolete queryAllBillInfo(Long billId, Integer billType) {
        String tablePref = InspectionConstants.getTablePref(false, billType);

        //1.查询磅单主表
        BillInfoObsolete billInfo = billDbMapper.selectBillById(tablePref, billId);

        if (Objects.isNull(billInfo)) {
            return billInfo;
        }
        //2.查询磅单材料
        List<BillMaterialInfoObsolete> materialList = billDbMapper.queryMaterialByBillId(tablePref, billId);
        boolean isAiBill = BooleanUtils.isTrue(billInfo.getIsAiBill());
        if (CollectionUtils.isNotEmpty(materialList) && isAiBill) {
            getMaterialAiInfo(materialList, billId, tablePref);
        }
        // 查询材料附加费集合
        List<BillMaterialSurchargeObsolete> materialSurchargeList = billDbMapper.queryBillMaterialSurcharge(tablePref, billId);
        //将附加费集合转为map  key：磅单材料行id  value: 材料对应的附加费集合
        Map<Long, List<BillMaterialSurchargeObsolete>> surchargeDataMap = MapUtils.EMPTY_MAP;
        if (!CollectionUtils.isEmpty(materialSurchargeList)) {
            surchargeDataMap = materialSurchargeList.stream().collect(Collectors.groupingBy(BillMaterialSurchargeObsolete::getBillMaterialRefId));
        }
        // 查询磅单材料额外信息
        List<BillMaterialExtraInfoObsolete> materialExtraInfoList = billDbMapper.queryBillMaterialExtra(tablePref, billId);
        Map<Long, BillMaterialExtraInfoObsolete> materialExtraInfoMap = MapUtils.EMPTY_MAP;
        if (!CollectionUtils.isEmpty(materialExtraInfoList)) {
            materialExtraInfoMap = materialExtraInfoList.stream().collect(Collectors.toMap(item -> item.getBillMaterialRefId(), item -> item));
        }

        if (materialList != null) {
            for (BillMaterialInfoObsolete billMaterialInfo : materialList) {
                Long billMaterialRowId = billMaterialInfo.getId();
                List<BillMaterialSurchargeObsolete> surchargeDataList = surchargeDataMap.get(billMaterialRowId);
                billMaterialInfo.setSurcharges(surchargeDataList);
                if (materialExtraInfoMap.containsKey(billMaterialRowId)) {
                    billMaterialInfo.setBillMaterialExtraInfo(materialExtraInfoMap.get(billMaterialRowId));
                }
            }
            billInfo.setBillMaterials(materialList);
        }
        //3.查询磅单图片
        List<BillPictureInfoObsolete> pictureList = billDbMapper.queryBillPicture(tablePref, billId);
        if (pictureList != null) {
            billInfo.setBillPictures(pictureList);
        }

        //4.查询磅单重复称重集合
        List<BillRepeatWeighInfoObsolete> repeatWeighList = billDbMapper.queryBillRepeatWeigh(tablePref, billId);
        if (repeatWeighList != null) {
            billInfo.setBillRepeatWeighs(repeatWeighList);
        }

        //5.查询磅单打印记录集合
        List<BillPrintHistoryInfoObsolete> printHistoryList = billDbMapper.queryBillPrintHistory(tablePref, billId);
        if (printHistoryList != null) {
            billInfo.setBillPrintHistoryInfos(printHistoryList);
        }
        //获取引用关系
        List<BizFlowRelation> bizFlowRelationList = bizFlowRelationMapper.getListByProjectIdAndBillIds(BizCodeEnum.ACCEPTANCE.getCode(), billInfo.getProjectId(), Collections.singletonList(billId));
        billInfo.setBizFlowRelationList(bizFlowRelationList);
        return billInfo;
    }

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
