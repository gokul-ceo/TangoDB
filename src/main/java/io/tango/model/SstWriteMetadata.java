package io.tango.model;

public class SstWriteMetadata {
    private long tableId;



    public SstWriteMetadata(long tableId) {
        this.tableId = tableId;

    }


    public long getTableId() {
        return tableId;
    }

    public void setTableId(long tableId) {
        this.tableId = tableId;
    }

}

