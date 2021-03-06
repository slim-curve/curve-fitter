/*
 * #%L
 * Curve Fitter library for fitting exponential decay curves to sample data.
 * %%
 * Copyright (C) 2010 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package loci.curvefitter;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.FloatByReference;

/**
 * TODO
 *
 * @author Aivar Grislis
 */
public class GrayCurveFitter extends AbstractCurveFitter {
    int m_algType;

    public interface CLibrary extends Library {

        public int nr_GCI_triple_integral_fitting_engine(float xincr, float y[], int fitStart, int fitEnd,
									   float instr[], int nInstr, int noise, float sig[],
                                                                           FloatByReference z, FloatByReference a, FloatByReference tau,
                                                                           float fitted[], float residuals[],
                                                                           FloatByReference chiSq, float chiSqTarget);

        public int nr_GCI_marquardt_fitting_engine(float xincr, float y[], int nData, int fitStart, int fitEnd,
                                                                           float instr[], int nInstr, int noise, float sig[],
                                                                           float param[], int paramFree[], int nParam,
                                                                           int restrainType, int fitType,
                                                                           float fitted[], float residuals[],
                                                                           FloatByReference chiSq);
        //,
        //                                                                   float covar[], float alpha[], float errAxes[],
        //                                                                   float chiSqTarget, int chiSqPercent);

        
        /*public int nr_GCI_marquardt_fitting_engine(float xincr, float *trans, int ndata, int fit_start, int fit_end,
								 float prompt[], int nprompt, //TODO ARG is this actually instr[] & ninstr?
								 noise_type noise, float sig[],
								 float param[], int paramfree[],
								 int nparam, restrain_type restrain,
								 fit_type fit, //TODO ARG void (*fitfunc)(float, float [], float *, float [], int),
								 float *fitted, float *residuals, float *chisq,
								 float **covar, float **alpha, float **erraxes,
									float chisq_target, int chisq_percent) {*/
    }

    @Override
    public int fitData(ICurveFitData[] dataArray) {
        CLibrary lib = (CLibrary) Native.loadLibrary("GrayCode", CLibrary.class);
        
        int start = dataArray[0].getDataStartIndex();
        int stop = dataArray[0].getTransEndIndex();

        //TODO ARG since initial x = fit_start * xincr we have to supply the unused portion of y[] before fit_start.
        // if this data were already premassaged it might be better to get rid of fit_start & _end, just give the
        // portion to be fitted and specify an initial x.
        //TODO ARG August use initial X of 0.

        if (FitAlgorithm.SLIMCURVE_RLD.equals(m_fitAlgorithm)) {
            float xincr = (float) m_xInc;
            int fitStart = start;
            int fitEnd = stop;
            float instr[] = new float[0];
            int nInstr = 0; // no lamp function
            int noise = 0; //2; // Poisson noise
            float sig[] = new float[stop+1];
            for (int i = 0; i < sig.length; ++i) {
                sig[i] = 1.0f; // ignore sig
            }
            FloatByReference z = new FloatByReference(1.0f); // starting guesses
            FloatByReference a = new FloatByReference(1.0f);
            FloatByReference tau = new FloatByReference(1.0f);
            float fitted[] = new float[stop+1];
            float residuals[] = new float[stop+1];
            FloatByReference chiSq = new FloatByReference();
            float chiSqTarget = 1.0f; //TODO note 'chiSqTarget' is internally known as 'division'!!!!

           // double[][] lmaData;

           // int length = stop - start + 1;
           // lmaData = new double[2][length];
            //double x_value = start * m_xInc;
            //for (int i = 0; i < length; ++i) {
            //    lmaData[0][i] = x_value;
            //    x_value += m_xInc;
            //}

            double params[] = new double[3];
            for (ICurveFitData data: dataArray) {
                float y[] = doubleToFloat(data.getTransient());

                //for (int i = 0; i < y.length; ++i) {
                //    System.out.println("y[" + i + "] is " + y[i]);
                //}

                params = data.getParams();
                a.setValue((float) params[0]);
                tau.setValue(1.0f / ((float) params[1])); // convert lambda to tau
                z.setValue((float) params[2]);



//System.out.println("GrayCurveFitter");
//System.out.println("!! xinc " + xincr + " start " + fitStart + " end " + fitEnd + " chiSqTarget " + chiSqTarget);
                int returnValue = lib.nr_GCI_triple_integral_fitting_engine(xincr, y, fitStart, fitEnd,
									   instr, nInstr, noise, sig,
                                                                           z, a, tau,
                                                                           fitted, residuals,
                                                                           chiSq, chiSqTarget);
//System.out.println("returnValue " + returnValue + " z " + z.getValue() + " a " + a.getValue() + " tau " + tau.getValue() + " chiSq " + chiSq.getValue());
                //data.setYFitted();
                params[0] = a.getValue();
                params[1] = 1.0 / tau.getValue(); // convert tau to lambda
                params[2] = z.getValue();
                data.setParams(params);
            }
        }
        else {
            float xincr = (float) m_xInc;
            int fitStart = start;
            int fitEnd = stop;
            float instr[] = new float[0];
            int nInstr = 0; // no lamp function
            int noise = 0; //2; // Poisson noise
            float sig[] = new float[stop+1];
            for (int i = 0; i < sig.length; ++i) {
                sig[i] = 1.0f; // ignore sig
            }
           // FloatByReference z = new FloatByReference(1.0f); // starting guesses
           // FloatByReference a = new FloatByReference(1.0f);
           // FloatByReference tau = new FloatByReference(1.0f);
            float fitted[] = new float[stop+1];
            float residuals[] = new float[stop+1];
            FloatByReference chiSq = new FloatByReference();
            float chiSqTarget = 1.0f; //TODO note 'chiSqTarget' is internally known as 'division'!!!!

           // double[][] lmaData;
//
           // int length = stop - start + 1;
           // lmaData = new double[2][length];
           // double x_value = start * m_xInc;
           // for (int i = 0; i < length; ++i) {
           //     lmaData[0][i] = x_value;
           //     x_value += m_xInc;
           // }

            // new for LMA
            double params[] = new double[3];

            for (ICurveFitData data: dataArray) {
                int nData = data.getTransient().length;
                float y[] = doubleToFloat(data.getTransient());

                //for (int i = 0; i < y.length; ++i) {
                //    System.out.println("y[" + i + "] is " + y[i]);
                //}

                float param[] = new float[3];
                param[0] = (float) data.getParams()[2];        // z
                param[1] = (float) data.getParams()[0];        // A
                //TODO param[2] = 1.0f / (float) data.getParams()[1]; // tau from lambda
                param[2] = (float) data.getParams()[1];

//System.out.println("Incoming params z " + data.getParams()[2] + " A " + data.getParams()[0] + " lambda " + data.getParams()[1]);
 
// float param[] = doubleToFloat(data.getParams());
                int nParams = param.length;
//System.out.println("nParams is " + nParams);
// param[1] = 1.0f / param[1];
                int paramFree[] = new int[nParams];
                for (int i = 0; i < nParams; ++i) {
                    paramFree[i] = 1;
                }
//System.out.println("paramFree length is " + paramFree.length);
                int restrainType = 0; //TODO 1 doesn't work; internally s/b calling "GCS_set_restrain_limits" but doesn't!!
                int fitType = 0;
                //float covar[][] = new float[stop+1][stop+1];
                //float alpha[][] = new float[stop+1][stop+1];
                //float errAxes[][] = new float[stop+1][stop+1];
                int chiSqPercent = 500;

                float covarX[] = new float[(stop+1)*(stop+1)];
                float alphaX[] = new float[(stop+1)*(stop+1)];
                float errAxesX[] = new float[(stop+1)*(stop+1)];

//System.out.println("xinc " + xincr + " start " + fitStart + " end " + fitEnd + " chiSqTarget " + chiSqTarget);
                int returnValue = lib.nr_GCI_marquardt_fitting_engine(xincr, y, nData, fitStart, fitEnd,
                                                                           instr, nInstr, noise, sig,
                                                                           param, paramFree, nParams,
                                                                           restrainType, fitType,
                                                                           fitted, residuals,
                                                                           chiSq);
                //,
                  //                                                         covarX, alphaX, errAxesX,
                    //                                                       chiSqTarget, chiSqPercent);






//System.out.println("HELLO returnValue z A tau = " + returnValue + " " + param[0] + " " + param[1] + " " + param[2]);
                data.setYFitted(floatToDouble(fitted));
                //data.setYFitted()
                double dParam[] = new double[3];
                dParam[0] = param[1];       // A
                //TODO ARG dParam[1] = 1.0 / param[2]; // lambda from tau
                dParam[1] = param[2];
                dParam[2] = param[0];       // z
                data.setParams(dParam);
//System.out.println("returning A " + dParam[0] + " lambda " + dParam[1] + " z " + dParam[2]);


//param[1] = 1.0f / param[1];
//data.setParams(floatToDouble(param));
            }

        }

        //TODO error return deserves more thought
        return 0;
    }

    double[] floatToDouble(float[] f) {
        double d[] = new double[f.length];
        for (int i = 0; i < f.length; ++i) {
            d[i] = f[i];
        }
        return d;
    }

    float[] doubleToFloat(double[] d) {
        float f[] = new float[d.length];
        for (int i = 0; i < d.length; ++i) {
            f[i] = (float) d[i];
        }
        return f;
    }

}
