package com.sunmi.innerprinter;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.InnerResultCallbcak;
import com.sunmi.peripheral.printer.SunmiPrinterService;

import java.util.HashMap;
import java.util.Map;

public class SunmiInnerPrinterModule extends ReactContextBaseJavaModule {
    public static ReactApplicationContext reactApplicationContext = null;
    private SunmiPrinterService woyouService;
    private BitmapUtils bitMapUtils;

    private static int NoSunmiPrinter = 0x00000000;
    private static int CheckSunmiPrinter = 0x00000001;
    private static int FoundSunmiPrinter = 0x00000002;
    private static int LostSunmiPrinter = 0x00000003;

    private PrinterReceiver receiver = new PrinterReceiver();


    private static final String TAG = "SunmiInnerPrinterModule";
    /**
     * sunmiPrinter means checking the printer connection status
     */
    public int sunmiPrinter = CheckSunmiPrinter;

    public SunmiInnerPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactApplicationContext = reactContext;
        initSunmiPrinterService(reactContext);
        bitMapUtils = new BitmapUtils(reactContext);
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ServiceActionConsts.INIT_ACTION);
        mFilter.addAction(ServiceActionConsts.FIRMWARE_UPDATING_ACITON);
        mFilter.addAction(ServiceActionConsts.NORMAL_ACTION);
        mFilter.addAction(ServiceActionConsts.ERROR_ACTION);
        mFilter.addAction(ServiceActionConsts.OUT_OF_PAPER_ACTION);
        mFilter.addAction(ServiceActionConsts.OVER_HEATING_ACITON);
        mFilter.addAction(ServiceActionConsts.NORMAL_HEATING_ACITON);
        mFilter.addAction(ServiceActionConsts.COVER_OPEN_ACTION);
        mFilter.addAction(ServiceActionConsts.COVER_ERROR_ACTION);
        mFilter.addAction(ServiceActionConsts.KNIFE_ERROR_1_ACTION);
        mFilter.addAction(ServiceActionConsts.KNIFE_ERROR_2_ACTION);
        mFilter.addAction(ServiceActionConsts.PRINTER_NON_EXISTENT_ACITON);
        mFilter.addAction(ServiceActionConsts.BLACKLABEL_NON_EXISTENT_ACITON);
        getReactApplicationContext().registerReceiver(receiver, mFilter);
        Log.d("PrinterReceiver", "------------ init ");
    }


    private InnerPrinterCallback innerPrinterCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            Log.i(TAG, "Sunmi inner printer service connected.");
            woyouService = service;
            checkSunmiPrinterService(service);
        }

        @Override
        protected void onDisconnected() {
            Log.i(TAG, "Sunmi inner printer service disconnected.");
            woyouService = null;
            sunmiPrinter = LostSunmiPrinter;
        }
    };

    /**
     * Check the printer connection,
     * like some devices do not have a printer but need to be connected to the cash drawer through a print service
     */
    private void checkSunmiPrinterService(SunmiPrinterService service) {
        boolean ret = false;
        try {
            ret = InnerPrinterManager.getInstance().hasPrinter(service);
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }
        sunmiPrinter = ret ? FoundSunmiPrinter : NoSunmiPrinter;
    }

    /**
     * init sunmi print service
     */
    public void initSunmiPrinterService(Context context) {
        try {
            boolean ret = InnerPrinterManager.getInstance().bindService(context, innerPrinterCallback);
            if (!ret) {
                sunmiPrinter = NoSunmiPrinter;
            }
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String getName() {
        return "SunmiInnerPrinter";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        final Map<String, Object> constantsChildren = new HashMap<>();

        constantsChildren.put("INIT_ACTION", ServiceActionConsts.INIT_ACTION);
        constantsChildren.put("FIRMWARE_UPDATING_ACITON", ServiceActionConsts.FIRMWARE_UPDATING_ACITON);
        constantsChildren.put("NORMAL_ACTION", ServiceActionConsts.NORMAL_ACTION);
        constantsChildren.put("ERROR_ACTION", ServiceActionConsts.ERROR_ACTION);
        constantsChildren.put("OUT_OF_PAPER_ACTION", ServiceActionConsts.OUT_OF_PAPER_ACTION);
        constantsChildren.put("OVER_HEATING_ACITON", ServiceActionConsts.OVER_HEATING_ACITON);
        constantsChildren.put("NORMAL_HEATING_ACITON", ServiceActionConsts.NORMAL_HEATING_ACITON);
        constantsChildren.put("COVER_OPEN_ACTION", ServiceActionConsts.COVER_OPEN_ACTION);
        constantsChildren.put("COVER_ERROR_ACTION", ServiceActionConsts.COVER_ERROR_ACTION);
        constantsChildren.put("KNIFE_ERROR_1_ACTION", ServiceActionConsts.KNIFE_ERROR_1_ACTION);
        constantsChildren.put("KNIFE_ERROR_2_ACTION", ServiceActionConsts.KNIFE_ERROR_2_ACTION);
        constantsChildren.put("PRINTER_NON_EXISTENT_ACITON", ServiceActionConsts.PRINTER_NON_EXISTENT_ACITON);
        constantsChildren.put("BLACKLABEL_NON_EXISTENT_ACITON", ServiceActionConsts.BLACKLABEL_NON_EXISTENT_ACITON);

        constants.put("Constants", constantsChildren);
        constants.put("hasPrinter", hasPrinter());

        try {
            constants.put("printerVersion", getPrinterVersion());
        } catch (Exception e) {
            // Log and ignore for it is not the madatory constants.
            Log.i(TAG, "ERROR: " + e.getMessage());
        }
        try {
            constants.put("printerSerialNo", getPrinterSerialNo());
        } catch (Exception e) {
            // Log and ignore for it is not the madatory constants.
            Log.i(TAG, "ERROR: " + e.getMessage());
        }
        try {
            constants.put("printerModal", getPrinterModal());
        } catch (Exception e) {
            // Log and ignore for it is not the madatory constants.
            Log.i(TAG, "ERROR: " + e.getMessage());
        }

        return constants;
    }


    /**
     * 初始化打印机，重置打印机的逻辑程序，但不清空缓存区数据，因此
     * 未完成的打印作业将在重置后继续
     *
     * @return
     */
    @ReactMethod
    public void printerInit(final Promise p) {
        final SunmiPrinterService printerService = woyouService;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    printerService.printerInit(new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 打印机自检，打印机会打印自检页
     *
     * @param callback 回调
     */
    @ReactMethod
    public void printerSelfChecking(final Promise p) {
        final SunmiPrinterService printerService = woyouService;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    printerService.printerSelfChecking(new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 获取打印机板序列号
     */
    @ReactMethod
    public void getPrinterSerialNo(final Promise p) {
        try {
            p.resolve(getPrinterSerialNo());
        } catch (Exception e) {
            Log.i(TAG, "ERROR: " + e.getMessage());
            p.reject("" + 0, e.getMessage());
        }
    }

    private String getPrinterSerialNo() throws Exception {
        final SunmiPrinterService printerService = woyouService;
        return printerService.getPrinterSerialNo();
    }

    /**
     * 获取打印机固件版本号
     */
    @ReactMethod
    public void getPrinterVersion(final Promise p) {
        try {
            p.resolve(getPrinterVersion());
        } catch (Exception e) {
            Log.i(TAG, "ERROR: " + e.getMessage());
            p.reject("" + 0, e.getMessage());
        }
    }

    private String getPrinterVersion() throws Exception {
        final SunmiPrinterService printerService = woyouService;
        return printerService.getPrinterVersion();
    }

    /**
     * 获取打印机型号
     */
    @ReactMethod
    public void getPrinterModal(final Promise p) {
        try {
            p.resolve(getPrinterModal());
        } catch (Exception e) {
            Log.i(TAG, "ERROR: " + e.getMessage());
            p.reject("" + 0, e.getMessage());
        }
    }

    private String getPrinterModal() throws Exception {
        //Caution: This method is not fully test -- Januslo 2018-08-11
        final SunmiPrinterService printerService = woyouService;
        return printerService.getPrinterModal();
    }

    @ReactMethod
    public void hasPrinter(final Promise p) {
        try {
            p.resolve(hasPrinter());
        } catch (Exception e) {
            Log.i(TAG, "ERROR: " + e.getMessage());
            p.reject("" + 0, e.getMessage());
        }
    }

    /**
     * 是否存在打印机服务
     * return {boolean}
     */
    private boolean hasPrinter() {
        final SunmiPrinterService printerService = woyouService;
        final boolean hasPrinterService = printerService != null;
        return hasPrinterService;
    }

    /**
     * 获取打印头打印长度
     */
    @ReactMethod
    public void getPrintedLength(final Promise p) {
        final SunmiPrinterService printerService = woyouService;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    printerService.getPrintedLength(new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 打印机走纸(强制换行，结束之前的打印内容后走纸n行)
     *
     * @param n:       走纸行数
     * @param callback 结果回调
     * @return
     */
    @ReactMethod
    public void lineWrap(int n, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final int count = n;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.lineWrap(count, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 使用原始指令打印
     *
     * @param data     指令
     * @param callback 结果回调
     */
    @ReactMethod
    public void sendRAWData(String base64EncriptedData, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final byte[] d = Base64.decode(base64EncriptedData, Base64.DEFAULT);
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.sendRAWData(d, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 设置对齐模式，对之后打印有影响，除非初始化
     *
     * @param alignment: 对齐方式 0--居左 , 1--居中, 2--居右
     * @param callback   结果回调
     */
    @ReactMethod
    public void setAlignment(int alignment, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final int align = alignment;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.setAlignment(align, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 设置打印字体, 对之后打印有影响，除非初始化
     * (目前只支持一种字体"gh"，gh是一种等宽中文字体，之后会提供更多字体选择)
     *
     * @param typeface: 字体名称
     */
    @ReactMethod
    public void setFontName(String typeface, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final String tf = typeface;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.setFontName(tf, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 设置字体大小, 对之后打印有影响，除非初始化
     * 注意：字体大小是超出标准国际指令的打印方式，
     * 调整字体大小会影响字符宽度，每行字符数量也会随之改变，
     * 因此按等宽字体形成的排版可能会错乱
     *
     * @param fontsize: 字体大小
     */
    @ReactMethod
    public void setFontSize(float fontsize, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final float fs = fontsize;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.setFontSize(fs, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }


    /**
     * 打印指定字体的文本，字体设置只对本次有效
     *
     * @param text:     要打印文字
     * @param typeface: 字体名称（目前只支持"gh"字体）
     * @param fontsize: 字体大小
     */
    @ReactMethod
    public void printTextWithFont(String text, String typeface, float fontsize, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final String txt = text;
        final String tf = typeface;
        final float fs = fontsize;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.printTextWithFont(txt, tf, fs, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 打印表格的一行，可以指定列宽、对齐方式
     *
     * @param colsTextArr  各列文本字符串数组
     * @param colsWidthArr 各列宽度数组(以英文字符计算, 每个中文字符占两个英文字符, 每个宽度大于0)
     * @param colsAlign    各列对齐方式(0居左, 1居中, 2居右)
     *                     备注: 三个参数的数组长度应该一致, 如果colsText[i]的宽度大于colsWidth[i], 则文本换行
     */
    @ReactMethod
    public void printColumnsText(ReadableArray colsTextArr, ReadableArray colsWidthArr, ReadableArray colsAlign, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        final String[] clst = new String[colsTextArr.size()];
        for (int i = 0; i < colsTextArr.size(); i++) {
            clst[i] = colsTextArr.getString(i);
        }
        final int[] clsw = new int[colsWidthArr.size()];
        for (int i = 0; i < colsWidthArr.size(); i++) {
            clsw[i] = colsWidthArr.getInt(i);
        }
        final int[] clsa = new int[colsAlign.size()];
        for (int i = 0; i < colsAlign.size(); i++) {
            clsa[i] = colsAlign.getInt(i);
        }
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.printColumnsText(clst, clsw, clsa, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }


    /**
     * 打印图片
     *
     * @param bitmap: 图片bitmap对象(最大宽度384像素，超过无法打印并且回调callback异常函数)
     */
    @ReactMethod
    public void printBitmap(String data, int width, int height, final Promise p) {
        try {
            final SunmiPrinterService ss = woyouService;
            byte[] decoded = Base64.decode(data, Base64.DEFAULT);
            final Bitmap bitMap = bitMapUtils.decodeBitmap(decoded, width, height);
            ThreadPoolManager.getInstance().executeTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        ss.printBitmap(bitMap, new InnerResultCallbcak() {
                            @Override
                            public void onPrintResult(int par1, String par2) {
                                Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                            }

                            @Override
                            public void onRunResult(boolean isSuccess) {
                                if (isSuccess) {
                                    p.resolve(null);
                                } else {
                                    p.reject("0", isSuccess + "");
                                }
                            }

                            @Override
                            public void onReturnString(String result) {
                                p.resolve(result);
                            }

                            @Override
                            public void onRaiseException(int code, String msg) {
                                p.reject("" + code, msg);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "ERROR: " + e.getMessage());
                        p.reject("" + 0, e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "ERROR: " + e.getMessage());
        }
    }

    /**
     * 打印一维条码
     *
     * @param data:         条码数据
     * @param symbology:    条码类型
     *                      0 -- UPC-A，
     *                      1 -- UPC-E，
     *                      2 -- JAN13(EAN13)，
     *                      3 -- JAN8(EAN8)，
     *                      4 -- CODE39，
     *                      5 -- ITF，
     *                      6 -- CODABAR，
     *                      7 -- CODE93，
     *                      8 -- CODE128
     * @param height:       条码高度, 取值1到255, 默认162
     * @param width:        条码宽度, 取值2至6, 默认2
     * @param textposition: 文字位置 0--不打印文字, 1--文字在条码上方, 2--文字在条码下方, 3--条码上下方均打印
     */
    @ReactMethod
    public void printBarCode(String data, int symbology, int height, int width, int textposition, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: ss:" + ss);
        final String d = data;
        final int s = symbology;
        final int h = height;
        final int w = width;
        final int tp = textposition;

        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.printBarCode(d, s, h, w, tp, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 打印二维条码
     *
     * @param data:       二维码数据
     * @param modulesize: 二维码块大小(单位:点, 取值 1 至 16 )
     * @param errorlevel: 二维码纠错等级(0 至 3)，
     *                    0 -- 纠错级别L ( 7%)，
     *                    1 -- 纠错级别M (15%)，
     *                    2 -- 纠错级别Q (25%)，
     *                    3 -- 纠错级别H (30%)
     */
    @ReactMethod
    public void printQRCode(String data, int modulesize, int errorlevel, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: ss:" + ss);
        final String d = data;
        final int size = modulesize;
        final int level = errorlevel;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.printQRCode(d, size, level, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 打印文字，文字宽度满一行自动换行排版，不满一整行不打印除非强制换行
     * 文字按矢量文字宽度原样输出，即每个字符不等宽
     *
     * @param text: 要打印的文字字符串
     */
    @ReactMethod
    public void printOriginalText(String text, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: " + text + " ss:" + ss);
        final String txt = text;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.printOriginalText(txt, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    /**
     * 打印缓冲区内容
     */
    @ReactMethod
    public void commitPrinterBuffer() {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: commit buffter ss:" + ss);
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.commitPrinterBuffer();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 进入缓冲模式，所有打印调用将缓存，调用commitPrinterBuffe()后打印
     *
     * @param clean: 是否清除缓冲区内容
     */
    @ReactMethod
    public void enterPrinterBuffer(boolean clean) {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: " + clean + " ss:" + ss);
        final boolean c = clean;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.enterPrinterBuffer(c);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 退出缓冲模式
     *
     * @param commit: 是否打印出缓冲区内容
     */
    @ReactMethod
    public void exitPrinterBuffer(boolean commit) {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: " + commit + " ss:" + ss);
        final boolean com = commit;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.exitPrinterBuffer(com);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                }
            }
        });
    }


    @ReactMethod
    public void printString(String message, final Promise p) {
        final SunmiPrinterService ss = woyouService;
        Log.i(TAG, "come: " + message + " ss:" + ss);
        final String msgs = message;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.printText(msgs, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int par1, String par2) {
                            Log.d(TAG, "ON PRINT RES: " + par1 + ", " + par2);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            if (isSuccess) {
                                p.resolve(null);
                            } else {
                                p.reject("0", isSuccess + "");
                            }
                        }

                        @Override
                        public void onReturnString(String result) {
                            p.resolve(result);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            p.reject("" + code, msg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    p.reject("" + 0, e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void clearBuffer() {
        final SunmiPrinterService ss = woyouService;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.clearBuffer();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void exitPrinterBufferWithCallback(final boolean commit, final Callback callback) {
        final SunmiPrinterService ss = woyouService;
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    ss.exitPrinterBufferWithCallback(commit, new InnerResultCallbcak() {
                        @Override
                        public void onPrintResult(int code, String msg) {
                            Log.d(TAG, "ON PRINT RES: " + code + ", " + msg);
                            if (code == 0)
                                callback.invoke(true);
                            else
                                callback.invoke(false);
                        }

                        @Override
                        public void onRunResult(boolean isSuccess) {
                            callback.invoke(isSuccess);
                        }

                        @Override
                        public void onReturnString(String result) {
                            // callback.invoke(isSuccess);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) {
                            callback.invoke(false);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "ERROR: " + e.getMessage());
                    callback.invoke(false);
                }
            }
        });
    }
}
