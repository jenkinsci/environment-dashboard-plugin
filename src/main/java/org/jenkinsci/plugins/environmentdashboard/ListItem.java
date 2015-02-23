package org.jenkinsci.plugins.environmentdashboard;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class ListItem extends AbstractDescribableImpl<ListItem> {

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

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ListItem>{
        @Override
        public String getDisplayName(){
            return "";
        }
    }

}
