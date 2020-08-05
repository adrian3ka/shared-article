package com.example;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.exception.PredictException;
import hex.genmodel.easy.prediction.BinomialModelPrediction;
import lombok.Builder;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;

public class Main {
  private static final List<String> features = Arrays.asList(
    "sku",
    "national_inv",
    "lead_time",
    "in_transit_qty",
    "forecast_3_month",
    "forecast_6_month",
    "forecast_9_month",
    "sales_1_month",
    "sales_3_month",
    "sales_6_month",
    "sales_9_month",
    "min_bank",
    "potential_issue",
    "pieces_past_due",
    "perf_6_month_avg",
    "perf_12_month_avg",
    "local_bo_qty",
    "deck_risk",
    "oe_constraint",
    "ppap_risk",
    "stop_auto_buy",
    "rev_stop"
  );

  @Builder
  private static class ReorderDataModel {
    @NonNull
    public final Double sku;

    @NonNull
    public final Double nationalInv;

    @NonNull
    public final Double leadTime;

    @NonNull
    public final Double inTransitQty;

    @NonNull
    public final Double forecast3Month;

    @NonNull
    public final Double forecast6Month;

    @NonNull
    public final Double forecast9Month;

    @NonNull
    public final Double sales1Month;

    @NonNull
    public final Double sales3Month;

    @NonNull
    public final Double sales6Month;

    @NonNull
    public final Double sales9Month;

    @NonNull
    public final Double minBank;

    @NonNull
    public final BooleanEnum potentialIssue;

    @NonNull
    public final Double piecesPastDue;

    @NonNull
    public final Double perf6MonthAvg;

    @NonNull
    public final Double perf12MonthAvg;

    @NonNull
    public final Double localBoQty;

    @NonNull
    public final BooleanEnum deckRisk;

    @NonNull
    public final BooleanEnum oeConstraint;

    @NonNull
    public final BooleanEnum ppapRisk;

    @NonNull
    public final BooleanEnum stopAutoBuy;

    @NonNull
    public final BooleanEnum revStop;
  }

  // First data on jupyter notebook
  /**
   * predict	No	      Yes
   * Yes	    0.203162	0.796838
   */
  private static final ReorderDataModel reorderDataModel1 = ReorderDataModel.builder()
    .sku(1113120D)
    .nationalInv(0D)
    .leadTime(8D)
    .inTransitQty(1D)
    .forecast3Month(6D)
    .forecast6Month(6D)
    .forecast9Month(6D)
    .sales1Month(0D)
    .sales3Month(4D)
    .sales6Month(9D)
    .sales9Month(12D)
    .minBank(0D)
    .potentialIssue(BooleanEnum.No)
    .piecesPastDue(1D)
    .perf6MonthAvg(0.9D)
    .perf12MonthAvg(0.89D)
    .localBoQty(0D)
    .deckRisk(BooleanEnum.No)
    .deckRisk(BooleanEnum.No)
    .oeConstraint(BooleanEnum.No)
    .ppapRisk(BooleanEnum.No)
    .stopAutoBuy(BooleanEnum.Yes)
    .revStop(BooleanEnum.No)
    .build();

  // 10th data on jupyter notebook
  /**
   * predict	No	      Yes
   * Yes	    0.0245814	0.975419
   */
  private static final ReorderDataModel reorderDataModel10 = ReorderDataModel.builder()
    .sku(1116870D)
    .nationalInv(-7D)
    .leadTime(8D)
    .inTransitQty(0D)
    .forecast3Month(56D)
    .forecast6Month(96D)
    .forecast9Month(112D)
    .sales1Month(13D)
    .sales3Month(30D)
    .sales6Month(56D)
    .sales9Month(76D)
    .minBank(0D)
    .potentialIssue(BooleanEnum.No)
    .piecesPastDue(0D)
    .perf6MonthAvg(0.97D)
    .perf12MonthAvg(0.92D)
    .localBoQty(7D)
    .deckRisk(BooleanEnum.No)
    .deckRisk(BooleanEnum.No)
    .oeConstraint(BooleanEnum.No)
    .ppapRisk(BooleanEnum.No)
    .stopAutoBuy(BooleanEnum.Yes)
    .revStop(BooleanEnum.No)
    .build();

  public static void main(String[] args) throws Exception {
    EasyPredictModelWrapper model = new EasyPredictModelWrapper(MojoModel.load("../StackedEnsemble_AllModels_AutoML_20200805_020054.zip"));

    List<ReorderDataModel> reorderDataModelList = Arrays
      .asList(
        reorderDataModel1, reorderDataModel10
      );


    reorderDataModelList.forEach(reorderDataModel -> {
      RowData row = new RowData();
      row.put("sku", reorderDataModel.sku);
      row.put("national_inv", reorderDataModel.nationalInv);
      row.put("lead_time", reorderDataModel.leadTime);
      row.put("in_transit_qty", reorderDataModel.inTransitQty);
      row.put("forecast_3_month", reorderDataModel.forecast3Month);
      row.put("forecast_6_month", reorderDataModel.forecast6Month);
      row.put("forecast_9_month", reorderDataModel.forecast9Month);
      row.put("sales_1_month", reorderDataModel.sales1Month);
      row.put("sales_3_month", reorderDataModel.sales3Month);
      row.put("sales_6_month", reorderDataModel.sales6Month);
      row.put("sales_9_month", reorderDataModel.sales9Month);
      row.put("min_bank", reorderDataModel.minBank);
      row.put("potential_issue", reorderDataModel.potentialIssue.toString());
      row.put("pieces_past_due", reorderDataModel.piecesPastDue);
      row.put("perf_6_month_avg", reorderDataModel.perf6MonthAvg);
      row.put("perf_12_month_avg", reorderDataModel.perf12MonthAvg);
      row.put("local_bo_qty", reorderDataModel.localBoQty);
      row.put("deck_risk", reorderDataModel.deckRisk.toString());
      row.put("oe_constraint", reorderDataModel.oeConstraint.toString());
      row.put("ppap_risk", reorderDataModel.ppapRisk.toString());
      row.put("stop_auto_buy", reorderDataModel.stopAutoBuy.toString());
      row.put("rev_stop", reorderDataModel.revStop.toString());

      BinomialModelPrediction p = null;

      try {
        p = model.predictBinomial(row);
      } catch (PredictException e) {
        e.printStackTrace();
      }

      System.out.println("User will reorder (1=yes; 0=no): " + p.label);
      System.out.print("Class probabilities: ");
      for (int i = 0; i < p.classProbabilities.length; i++) {
        if (i > 0) {
          System.out.print(",");
        }
        System.out.print(p.classProbabilities[i]);
      }
      System.out.println("");
    });
  }
}