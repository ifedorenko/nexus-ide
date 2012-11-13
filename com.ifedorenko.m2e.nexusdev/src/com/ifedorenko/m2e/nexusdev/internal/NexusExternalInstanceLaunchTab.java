package com.ifedorenko.m2e.nexusdev.internal;

import static com.ifedorenko.m2e.nexusdev.internal.NexusExternalLaunchDelegate.ATTR_APPLICATION_PORT;
import static com.ifedorenko.m2e.nexusdev.internal.NexusExternalLaunchDelegate.ATTR_INSTALLATION_LOCATION;
import static com.ifedorenko.m2e.nexusdev.internal.NexusExternalLaunchDelegate.ATTR_WORKDIR_LOCATION;

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

    private Text workdirLocation;

    private Text applicationPort;

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

        Label lblWorkdirLocation = new Label( composite, SWT.NONE );
        lblWorkdirLocation.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblWorkdirLocation.setText( "Workdir location" );

        workdirLocation = new Text( composite, SWT.BORDER );
        workdirLocation.addVerifyListener( new VerifyListener()
        {
            public void verifyText( VerifyEvent e )
            {
                setDirty( true );
                updateLaunchConfigurationDialog();
            }
        } );
        workdirLocation.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        new Label( composite, SWT.NONE );

        Label lblApplicationPort = new Label( composite, SWT.NONE );
        lblApplicationPort.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblApplicationPort.setText( "Application port" );

        applicationPort = new Text( composite, SWT.BORDER );
        applicationPort.addVerifyListener( new VerifyListener()
        {
            public void verifyText( VerifyEvent e )
            {
                setDirty( true );
                updateLaunchConfigurationDialog();
            }
        } );
        applicationPort.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );
        new Label( composite, SWT.NONE );
    }

    @Override
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration )
    {
    }

    @Override
    public void initializeFrom( ILaunchConfiguration configuration )
    {
        initializeFrom( configuration, installationLocation, ATTR_INSTALLATION_LOCATION );
        initializeFrom( configuration, workdirLocation, ATTR_WORKDIR_LOCATION );
        initializeFrom( configuration, applicationPort, ATTR_APPLICATION_PORT );
    }

    private void initializeFrom( ILaunchConfiguration configuration, Text field, String attr )
    {
        String value;
        try
        {
            value = configuration.getAttribute( attr, "" );
        }
        catch ( CoreException e )
        {
            value = "";
        }
        field.setText( value );
    }

    @Override
    public void performApply( ILaunchConfigurationWorkingCopy configuration )
    {
        configuration.setAttribute( ATTR_INSTALLATION_LOCATION, toString( installationLocation ) );
        configuration.setAttribute( ATTR_WORKDIR_LOCATION, toString( workdirLocation ) );
        configuration.setAttribute( ATTR_APPLICATION_PORT, toString( applicationPort ) );
    }

    private String toString( Text field )
    {
        String text = field.getText();
        return text != null && !text.trim().isEmpty() ? text : null;
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
        // String location = installationLocation.getText();
        //
        // if ( location == null || "".equals( location.trim() ) )
        // {
        // return false;
        // }
        //
        // return new File( location ).isDirectory();
    }
}
