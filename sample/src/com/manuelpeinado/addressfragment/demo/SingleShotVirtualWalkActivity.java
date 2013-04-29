package com.manuelpeinado.addressfragment.demo;


public class SingleShotVirtualWalkActivity extends MapActivity {

    @Override
    protected boolean getUseMockLocationSource() {
        return true;
    }
    
    @Override
    protected boolean getIsSingleShot() {
        return true;
    }
}
