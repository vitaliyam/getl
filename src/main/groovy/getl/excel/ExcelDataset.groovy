package getl.excel

import getl.data.Connection
import getl.data.Dataset

/**
 * Excel Dataset class
 * @author Dmitry Shaldin
 */
class ExcelDataset extends Dataset {
    ExcelDataset () {
        super()
    }

    @Override
    void setConnection(Connection value) {
        assert value == null || value instanceof ExcelConnection
        super.setConnection(value)
    }

    /**
     * List name
     * @return
     */
    String getListName () { params.listName }
    void setListName (String value) { params.listName = value }

    /**
     * Offset param
     * @return
     */
    int getOffset() { params.offset }
    void setOffset(final Map<String, Integer> value) { params.offset = value }

    /**
     * Limit rows to return
     * @return
     */
    int getLimit() { params.limit }
    void setLimit(int value) { params.limit = value }

    /**
     * Header row
     * @return
     */
    boolean getHeader() { params.header }
    void setHeader(boolean value) { params.header = value }
}
