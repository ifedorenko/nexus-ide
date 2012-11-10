package com.ifedorenko.m2e.nexusdev.internal;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NexusExternalInstanceLaunchTab
    extends AbstractLaunchConfigurationTab
{
    private Text installationLocation;

    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public void createControl( Composite parent )
    {
        Composite composite = new Composite( parent, SWT.NONE );
        setControl( composite );
        composite.setLayout( new GridLayout( 3, false ) );

        Label lblInstallationLocation = new Label( composite, SWT.NONE );
        lblInstallationLocation.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblInstallationLocation.setText( "Installation location" );

        installationLocation = new Text( composite, SWT.BORDER );
        installationLocation.addVerifyListener( new VerifyListener()
        {
            public void verifyText( VerifyEvent e )
            {
                setDirty( true );
                updateLaunchConfigurationDialog();
            }
        } );
        installationLocation.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Button btnBrowseInstallationLocation = new Button( composite, SWT.NONE );
        btnBrowseInstallationLocation.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                DirectoryDialog dialog = new DirectoryDialog( getShell() );
                String location = dialog.open();
                if ( location != null )
                {
                    installationLocation.setText( location );
                }
            }
        } );
        btnBrowseInstallationLocation.setText( "Browse..." );
    }

    @Override
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration )
    {
    }

    @Override
    public void initializeFrom( ILaunchConfiguration configuration )
    {
        String location;
        try
        {
            location = configuration.getAttribute( NexusExternalLaunchDelegate.ATTR_INSTALLATION_LOCATION, "" );
        }
        catch ( CoreException e )
        {
            location = "";
        }
        installationLocation.setText( location );
    }

    @Override
    public void performApply( ILaunchConfigurationWorkingCopy configuration )
    {
        configuration.setAttribute( NexusExternalLaunchDelegate.ATTR_INSTALLATION_LOCATION,
                                    installationLocation.getText() );
    }

    @Override
    public String getName()
    {
        return "Nexus installation";
    }

    @Override
    public boolean canSave()
    {
        return true;
//        String location = installationLocation.getText();
//
//        if ( location == null || "".equals( location.trim() ) )
//        {
//            return false;
//        }
//
//        return new File( location ).isDirectory();
    }
}
