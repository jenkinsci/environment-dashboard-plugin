package org.jenkinsci.plugins.environmentdashboard;

import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class ListItem {
    public String columnName;
    public String contents;

    @DataBoundConstructor
    public ListItem(String columnName, String contents){
        this.columnName = columnName;
        this.contents = contents;
    }

    public String getColumnName(){
        return columnName;
    }

    public String getContents(){
        return contents;
    }


}
