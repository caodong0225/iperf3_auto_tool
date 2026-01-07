package com.github.jyzxc.autoiperf.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshProfile {
    private String profileName;
    private String host;
    private String username;
    private String password;

    @Override
    public String toString() {
        // This is what will be displayed in the JComboBox
        return profileName;
    }
}
