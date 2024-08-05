import React, { useState, useEffect, useRef, useCallback } from 'react';
import Select from 'react-select';

const ConnectOverview = () => {
    const [options, setOptions] = useState([]);
    const [features, setFeatures] = useState([]);
    const [installDisabled, setInstallDisabled] = useState(true);
    const [refreshOptions, setRefreshOptions] = useState(false);
    const [file, setFile] = useState(null);
    const [activeTab, setActiveTab] = useState('dropdown');
    const [warningMessage, setWarningMessage] = useState('');
    const [featureStatus, setFeatureStatus] = useState([]); // Track feature statuses
    const fileInputRef = useRef(null); // Ref for the file input element

    // Accepted file names and types
    const [acceptedFileNames, setAcceptedFileNames] = useState([]);
    const acceptedFileTypes = ['application/zip', 'application/x-zip-compressed'];

    // Handle file selection
    const handleFileChange = (event) => {
        const selectedFile = event.target.files[0];
        if (selectedFile) {
            const isValidType = acceptedFileTypes.includes(selectedFile.type);
            const isValidName = acceptedFileNames.includes(selectedFile.name);

            if (isValidType && isValidName) {
                setFile(selectedFile);
                setWarningMessage(''); // Clear warning message
            } else {
                setFile(null);
                setWarningMessage(
                    !isValidType
                        ? 'Invalid file type. Please upload a ZIP file.'
                        : 'Invalid file name. Please upload an accepted file.'
                );
                if (fileInputRef.current) {
                    fileInputRef.current.value = ''; // Clear the file input value
                }
            }
        }
    };

    // Handle file upload with useCallback to avoid re-creation on every render
    const handleFileUpload = useCallback(() => {
        if (file) {
            const confirmationMessage = `Are you sure you want to install the file "${file.name}"?`;

            if (window.confirm(confirmationMessage)) {
                console.log('Uploading file:', file.name);

                fetch(`/data/hce/installFile/${encodeURIComponent(file.name)}`, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/zip',
                    },
                    body: file
                })
                .then(response => response.json())
                .then((data) => {
                    console.log('Upload successful', data);
                    alert("File installed successfully");
                    setFile(null);
                    if (fileInputRef.current) {
                        fileInputRef.current.value = ''; // Clear the file input value
                    }
                })
                .catch((error) => {
                    console.error('Upload failed', error);
                    alert("File upload failed");
                    setFile(null);
                    if (fileInputRef.current) {
                        fileInputRef.current.value = ''; // Clear the file input value
                    }
                });
            } else {
                console.log("File upload cancelled");
            }
        } else {
            console.log('No file selected');
        }
    }, [file]); // Dependency on file

    // Fetch feature status from server
    const fetchFeatureStatus = () => {
        fetch(`/data/hce/featureStatus`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            if (json.features) {
                setFeatureStatus(json.features);

                 // Populate accepted file names with features
                 setAcceptedFileNames(json.features.map(feature => feature.name + ".zip"));
            } else {
                console.error('Error: No feature status data found.');
            }
        })
        .catch(error => {
            console.error('Error fetching feature status:', error);
        });
    };

    // Function to fetch dropdown options
    const fetchFeatureOptions = () => {
        fetch(`/data/hce/featureStatus`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            if (json.features) {

                // Filter features where isActive is true and isInstalled is false
                const formattedOptions = json
                    .features
                    .filter(feature => feature.activeStatus && !feature.installStatus)
                    .map(feature => ({
                    value: feature.name,
                    label: feature.name
                }));
    
                setOptions(formattedOptions);
    
                // Determine if any options are available to enable or disable the install button
                setInstallDisabled(formattedOptions.length === 0);
            } else {
                console.error('Error: No features data found.');
            }
        })
        .catch(error => {
            console.error('Error fetching feature status:', error);
        });
    };

    useEffect(() => {
        fetchFeatureOptions();
        fetchFeatureStatus();
    }, [refreshOptions]);

    const handleDropdownChange = (selectedFeatures) => {
        setFeatures(selectedFeatures || []);
        setInstallDisabled(selectedFeatures.length === 0);
    };

    const install = () => {
        let params = features.map((a) => a.value);
        let queryString = `?params=${params.join(',')}`;

        fetch(`/data/hce/install/${queryString}`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            console.log("install JSON Response: ");
            console.log(json);
        });

        setFeatures([]);
        setRefreshOptions(prev => !prev);
    };

    const resetFeatures = () => {
        fetch(`/data/hce/resetFeatures`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        });
        window.location.reload();
    };

    const showConfirmation = () => {
        let confirmationMessage = "Are you sure you want to install ";
        if (features.length === 1) {
            confirmationMessage += `${features[0].label}?`;
        } else if (features.length > 1) {
            const featureLabels = features.map(feature => feature.label);
            const lastFeature = featureLabels.pop();

            confirmationMessage += featureLabels.join(', ') + ` and ${lastFeature}?`;
        } else {
            confirmationMessage += "these features?";
        }

        if (window.confirm(confirmationMessage)) {
            install();
            alert("Installed");
        } else {
            console.log("Install cancelled");
        }
    };

    return (
        <div>
            {/* Status Component */}
            <div style={{ marginBottom: '20px' }}>
                <h2>Feature Status</h2>
                <ul>
                    {featureStatus.map((feature, index) => (
                        <li key={index} style={{ marginBottom: '10px' }}>
                            <strong>{feature.name}</strong>: {feature.activeStatus ? 'Active' : 'Inactive'} / {feature.installStatus ? 'Installed' : 'Not Installed'}
                        </li>
                    ))}
                </ul>
            </div>
            {/* Tabs */}
            <div style={{ marginBottom: '20px' }}>
                <button
                    style={{
                        marginRight: '10px',
                        padding: '10px',
                        cursor: 'pointer',
                        backgroundColor: activeTab === 'dropdown' ? '#2199e8' : '#e0e0e0',
                        color: activeTab === 'dropdown' ? 'white' : 'black',
                        border: 'none',
                        borderRadius: '5px',
                        fontSize: '16px',
                        fontWeight: 'bold'
                    }}
                    onClick={() => setActiveTab('dropdown')}
                >
                    Dropdown Selection
                </button>
                <button
                    style={{
                        padding: '10px',
                        cursor: 'pointer',
                        backgroundColor: activeTab === 'upload' ? '#2199e8' : '#e0e0e0',
                        color: activeTab === 'upload' ? 'white' : 'black',
                        border: 'none',
                        borderRadius: '5px',
                        fontSize: '16px',
                        fontWeight: 'bold'
                    }}
                    onClick={() => setActiveTab('upload')}
                >
                    File Upload
                </button>
            </div>
            
            {/* Dropdown */}
            {activeTab === 'dropdown' && (
                <div>
                    <h2>Select Module</h2>
                    <Select
                        options={options}
                        onChange={handleDropdownChange}
                        value={features}
                        isMulti
                    />
                    <button
                        style={{
                            padding: '10px 20px',
                            backgroundColor: installDisabled ? '#cccccc' : '#2199e8',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: installDisabled ? 'not-allowed' : 'pointer',
                            fontSize: '16px',
                            fontWeight: 'bold'
                        }}
                        onClick={showConfirmation}
                        disabled={installDisabled}
                    >
                        Install
                    </button>
                    <button
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#2199e8',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer',
                            fontSize: '16px',
                            fontWeight: 'bold'
                        }}
                        onClick={resetFeatures}
                    >
                        Reset
                    </button>
                </div>
            )}

            {/* File Upload */}
            {activeTab === 'upload' && (
                <div>
                    <h2>Upload Module File</h2>
                    <input
                        type="file"
                        ref={fileInputRef}
                        onChange={handleFileChange}
                    />
                    <button 
                        style={{
                            padding: '10px 20px',
                            backgroundColor: '#2199e8',
                            color: 'white',
                            border: 'none',
                            borderRadius: '5px',
                            cursor: 'pointer',
                            fontSize: '16px',
                            fontWeight: 'bold'
                        }}
                        onClick={handleFileUpload}
                    >
                        Upload File
                    </button>
                    {file && <p style={{ marginTop: '10px', fontSize: '16px' }}>Selected file: {file.name}</p>}
                    {warningMessage && <p style={{ color: 'red', marginTop: '10px' }}>{warningMessage}</p>}
                </div>
            )}
        </div>
    );
};

export default ConnectOverview;
